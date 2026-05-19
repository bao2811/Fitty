import { createSign, randomUUID } from "node:crypto";
import { mkdtempSync, readFileSync, existsSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { extname } from "node:path";
import { join } from "node:path";
import { spawnSync } from "node:child_process";

const ROOT_ENV_PATH = ".env";
const GOOGLE_OAUTH_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
const FIRESTORE_BASE = "https://firestore.googleapis.com/v1";
const STORAGE_UPLOAD_BASE = "https://storage.googleapis.com/upload/storage/v1";
const STORAGE_OBJECT_BASE = "https://storage.googleapis.com/storage/v1";

async function main() {
  const options = parseArgs(process.argv.slice(2));
  const env = loadDotEnv(ROOT_ENV_PATH);
  const serviceAccountPath = requiredEnv(env, "FIREBASE_SERVICE_ACCOUNT_PATH");
  const serviceAccount = JSON.parse(readFileSync(serviceAccountPath, "utf8"));
  const projectId = env.FIREBASE_PROJECT_ID || serviceAccount.project_id;
  const bucket = env.FIREBASE_STORAGE_BUCKET || `${projectId}.firebasestorage.app`;
  const workoutBaseUrl = stripTrailingSlash(requiredEnv(env, "WORKOUTX_BASE_URL"));
  const workoutApiKey = requiredEnv(env, "WORKOUTX_API_KEY");
  const workoutGate = createWorkoutXGate(Number(env.MIN_REQUEST_INTERVAL_MS || 2200));

  const accessToken = await fetchGoogleAccessToken(serviceAccount);
  if (options.wipeStorage) {
    await deleteAllStorageObjects({ accessToken, bucket });
    console.log(`Deleted all existing objects from bucket ${bucket}.`);
  }
  const exercises = await fetchAllWorkoutExercises({
    workoutBaseUrl,
    workoutApiKey,
    offset: options.offset,
    limit: options.limit,
    pageSize: options.pageSize,
    workoutGate
  });

  console.log(`Fetched ${exercises.length} exercises from WorkoutX.`);

  let uploadedThumbnails = 0;
  let uploadedGifs = 0;

  for (let index = 0; index < exercises.length; index += 1) {
    const exercise = exercises[index];
    const exerciseId = String(exercise.id || "").trim();
    if (!exerciseId) {
      console.warn(`Skipping exercise at index ${index}: missing id.`);
      continue;
    }
    const thumbnailUpload = await uploadThumbnailAsset({
      accessToken,
      bucket,
      exerciseId,
      exercise,
      workoutBaseUrl,
      workoutApiKey,
      workoutGate
    });
    if (thumbnailUpload.uploaded) uploadedThumbnails += 1;

    const gifUpload = options.uploadGifs
      ? await uploadRemoteAssetIfPresent({
          accessToken,
          bucket,
          objectPath: buildGifObjectPath(exercise, exercise.gifUrl),
          remoteUrl: exercise.gifUrl,
          workoutBaseUrl,
          workoutApiKey,
          workoutGate
        })
      : {
          uploaded: false,
          storagePath: "",
          downloadUrl: "",
          contentType: ""
        };
    if (gifUpload.uploaded) uploadedGifs += 1;

    const firestoreDocument = buildExerciseFirestoreDocument({
      exercise,
      fetchedAt: new Date().toISOString(),
      thumbnailUpload,
      gifUpload
    });

    await upsertFirestoreDocument({
      accessToken,
      projectId,
      collection: "exercises",
      documentId: exerciseId,
      fields: firestoreDocument
    });

    console.log(`[${index + 1}/${exercises.length}] synced ${exerciseId}`);
  }

  console.log(`Done. Uploaded ${uploadedThumbnails} thumbnails and ${uploadedGifs} gifs.`);
}

function parseArgs(argv) {
  const options = {
    offset: 0,
    limit: null,
    pageSize: 100,
    uploadGifs: false,
    wipeStorage: false
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--offset") options.offset = Number(argv[++index] || 0);
    else if (arg === "--limit") options.limit = Number(argv[++index] || 0);
    else if (arg === "--page-size") options.pageSize = Number(argv[++index] || 100);
    else if (arg === "--upload-gifs") options.uploadGifs = true;
    else if (arg === "--wipe-storage") options.wipeStorage = true;
  }

  return options;
}

function loadDotEnv(path) {
  if (!existsSync(path)) return {};
  const lines = readFileSync(path, "utf8").split(/\r?\n/);
  return Object.fromEntries(
    lines
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith("#") && line.includes("="))
      .map((line) => {
        const index = line.indexOf("=");
        const key = line.slice(0, index).trim();
        const value = line.slice(index + 1).trim().replace(/^"(.*)"$/, "$1");
        return [key, value];
      })
  );
}

function requiredEnv(env, key) {
  const value = env[key];
  if (!value) {
    throw new Error(`Missing required env var ${key}`);
  }
  return value;
}

function stripTrailingSlash(value) {
  return value.endsWith("/") ? value.slice(0, -1) : value;
}

async function fetchGoogleAccessToken(serviceAccount) {
  const nowSeconds = await fetchGoogleNowSeconds(serviceAccount.token_uri);
  const issuedAt = nowSeconds - 120;
  const jwtHeader = base64UrlEncode(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const jwtPayload = base64UrlEncode(JSON.stringify({
    iss: serviceAccount.client_email,
    scope: GOOGLE_OAUTH_SCOPE,
    aud: serviceAccount.token_uri,
    exp: issuedAt + 3500,
    iat: issuedAt
  }));
  const signer = createSign("RSA-SHA256");
  signer.update(`${jwtHeader}.${jwtPayload}`);
  signer.end();
  const signature = signer.sign(serviceAccount.private_key).toString("base64url");
  const assertion = `${jwtHeader}.${jwtPayload}.${signature}`;

  const response = await fetch(serviceAccount.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion
    })
  });
  if (!response.ok) {
    throw new Error(`Failed to fetch Google access token: ${response.status} ${await response.text()}`);
  }
  const payload = await response.json();
  return payload.access_token;
}

async function fetchGoogleNowSeconds(tokenUri) {
  try {
    const response = await fetch(tokenUri, { method: "HEAD" });
    const dateHeader = response.headers.get("date");
    if (dateHeader) {
      const millis = Date.parse(dateHeader);
      if (Number.isFinite(millis)) {
        return Math.floor(millis / 1000);
      }
    }
  } catch {
    // Fall back to local time below.
  }
  return Math.floor(Date.now() / 1000);
}

async function fetchAllWorkoutExercises({ workoutBaseUrl, workoutApiKey, offset, limit, pageSize, workoutGate }) {
  const items = [];
  let currentOffset = offset;
  let remaining = limit;
  let total = Number.POSITIVE_INFINITY;

  while (currentOffset < total && (remaining == null || remaining > 0)) {
    const batchSize = remaining == null ? pageSize : Math.min(pageSize, remaining);
    const url = new URL(`${workoutBaseUrl}/v1/exercises`);
    url.searchParams.set("offset", String(currentOffset));
    url.searchParams.set("limit", String(batchSize));
    url.searchParams.set("api-key", workoutApiKey);

    const response = await fetchWorkoutX(url.toString(), {
      headers: {
        "X-WorkoutX-Key": workoutApiKey
      },
      workoutGate
    });
    if (!response.ok) {
      throw new Error(`WorkoutX fetch failed: ${response.status} ${await response.text()}`);
    }

    const page = await response.json();
    const pageItems = Array.isArray(page.data) ? page.data : Array.isArray(page.items) ? page.items : [];
    items.push(...pageItems);
    total = Number(page.total || items.length);
    currentOffset += pageItems.length;
    if (remaining != null) remaining -= pageItems.length;
    if (pageItems.length === 0) break;
  }

  return items;
}

function buildGifObjectPath(exercise, remoteUrl) {
  const extension = normalizeExtension(remoteUrl, "gif");
  return `${buildExerciseFolder(exercise)}/gif${extension}`;
}

function buildThumbnailObjectPath(exercise, remoteUrl) {
  const extension = normalizeExtension(remoteUrl, "thumbnail");
  return `exercise-thumbnails/${slugify(exercise.bodyPart || exercise.muscleGroup || "other")}/${slugify(String(exercise.id || "unknown"))}-${slugify(exercise.name || "exercise")}${extension}`;
}

function buildExerciseFolder(exercise) {
  const bodyPart = slugify(exercise.bodyPart || exercise.muscleGroup || "other");
  const exerciseName = slugify(exercise.name || String(exercise.id || "exercise"));
  const exerciseId = slugify(String(exercise.id || "unknown"));
  return `exercises/${bodyPart}/${exerciseName}-${exerciseId}`;
}

function slugify(value) {
  return String(value)
    .normalize("NFKD")
    .replace(/[^\w\s-]/g, "")
    .trim()
    .replace(/[\s_-]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .toLowerCase() || "unknown";
}

function normalizeExtension(remoteUrl, assetKind) {
  if (!remoteUrl) {
    return assetKind === "gif" ? ".gif" : ".jpg";
  }
  try {
    const pathname = new URL(remoteUrl).pathname;
    const extension = extname(pathname).toLowerCase();
    if (extension) return extension;
  } catch {
    return assetKind === "gif" ? ".gif" : ".jpg";
  }
  return assetKind === "gif" ? ".gif" : ".jpg";
}

async function uploadRemoteAssetIfPresent({
  accessToken,
  bucket,
  objectPath,
  remoteUrl,
  workoutBaseUrl,
  workoutApiKey,
  workoutGate
}) {
  if (!remoteUrl) {
    return {
      uploaded: false,
      storagePath: "",
      downloadUrl: "",
      contentType: ""
    };
  }

  const remoteAsset = await downloadRemoteAsset({
    remoteUrl,
    workoutBaseUrl,
    workoutApiKey,
    workoutGate
  });
  const downloadToken = randomUUID();
  const encodedObjectName = encodeURIComponent(objectPath);

  const uploadResponse = await fetch(
    `${STORAGE_UPLOAD_BASE}/b/${bucket}/o?uploadType=media&name=${encodedObjectName}`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": remoteAsset.contentType || inferContentType(objectPath)
      },
      body: remoteAsset.buffer
    }
  );
  if (!uploadResponse.ok) {
    throw new Error(`Failed to upload ${objectPath}: ${uploadResponse.status} ${await uploadResponse.text()}`);
  }

  const metadataResponse = await fetch(
    `${STORAGE_OBJECT_BASE}/b/${bucket}/o/${encodedObjectName}`,
    {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        metadata: {
          firebaseStorageDownloadTokens: downloadToken
        }
      })
    }
  );
  if (!metadataResponse.ok) {
    throw new Error(`Failed to patch metadata for ${objectPath}: ${metadataResponse.status} ${await metadataResponse.text()}`);
  }

  return {
    uploaded: true,
    storagePath: objectPath,
    downloadUrl: `https://firebasestorage.googleapis.com/v0/b/${bucket}/o/${encodedObjectName}?alt=media&token=${downloadToken}`,
    contentType: remoteAsset.contentType || inferContentType(objectPath)
  };
}

async function uploadThumbnailAsset({
  accessToken,
  bucket,
  exerciseId,
  exercise,
  workoutBaseUrl,
  workoutApiKey,
  workoutGate
}) {
  if (exercise.thumbnailUrl) {
    return uploadRemoteAssetIfPresent({
      accessToken,
      bucket,
      objectPath: buildThumbnailObjectPath(exercise, exercise.thumbnailUrl),
      remoteUrl: exercise.thumbnailUrl,
      workoutBaseUrl,
      workoutApiKey,
      workoutGate
    });
  }

  if (!exercise.gifUrl) {
    return {
      uploaded: false,
      storagePath: "",
      downloadUrl: "",
      contentType: ""
    };
  }

  const gifAsset = await downloadRemoteAsset({
    remoteUrl: exercise.gifUrl,
    workoutBaseUrl,
    workoutApiKey,
    workoutGate
  });

  const tempDirectory = mkdtempSync(join(tmpdir(), "fitty-gif-thumb-"));
  const gifPath = join(tempDirectory, `${exerciseId}.gif`);
  const pngPath = join(tempDirectory, `${exerciseId}.png`);

  try {
    writeFileSync(gifPath, gifAsset.buffer);
    const extraction = spawnSync("python", ["scripts/extract_gif_first_frame.py", gifPath, pngPath], {
      cwd: process.cwd(),
      encoding: "utf8"
    });
    if (extraction.status !== 0) {
      throw new Error(extraction.stderr || extraction.stdout || "Failed to extract thumbnail from gif");
    }
    const pngBuffer = readFileSync(pngPath);
    return uploadBufferToStorage({
      accessToken,
      bucket,
      objectPath: buildThumbnailObjectPath(exercise, "thumbnail.png"),
      buffer: pngBuffer,
      contentType: "image/png"
    });
  } finally {
    rmSync(tempDirectory, { recursive: true, force: true });
  }
}

async function downloadRemoteAsset({
  remoteUrl,
  workoutBaseUrl,
  workoutApiKey,
  workoutGate
}) {
  for (;;) {
    try {
      const remoteRequest = buildRemoteAssetRequest(remoteUrl, workoutBaseUrl, workoutApiKey);
      const remoteResponse = await fetchWorkoutX(remoteRequest.url, {
        headers: remoteRequest.headers,
        workoutGate
      });
      if (!remoteResponse.ok) {
        throw new Error(`Failed to download asset ${remoteUrl}: ${remoteResponse.status} ${await remoteResponse.text()}`);
      }

      return {
        buffer: Buffer.from(await remoteResponse.arrayBuffer()),
        contentType: remoteResponse.headers.get("content-type") || inferContentType(remoteUrl)
      };
    } catch (error) {
      console.warn(`Asset download failed for ${remoteUrl}: ${error.message}. Retrying in 5000 ms.`);
      await sleep(5000);
    }
  }
}

async function uploadBufferToStorage({
  accessToken,
  bucket,
  objectPath,
  buffer,
  contentType
}) {
  const downloadToken = randomUUID();
  const encodedObjectName = encodeURIComponent(objectPath);

  const uploadResponse = await fetch(
    `${STORAGE_UPLOAD_BASE}/b/${bucket}/o?uploadType=media&name=${encodedObjectName}`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": contentType
      },
      body: buffer
    }
  );
  if (!uploadResponse.ok) {
    throw new Error(`Failed to upload ${objectPath}: ${uploadResponse.status} ${await uploadResponse.text()}`);
  }

  const metadataResponse = await fetch(
    `${STORAGE_OBJECT_BASE}/b/${bucket}/o/${encodedObjectName}`,
    {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        metadata: {
          firebaseStorageDownloadTokens: downloadToken
        }
      })
    }
  );
  if (!metadataResponse.ok) {
    throw new Error(`Failed to patch metadata for ${objectPath}: ${metadataResponse.status} ${await metadataResponse.text()}`);
  }

  return {
    uploaded: true,
    storagePath: objectPath,
    downloadUrl: `https://firebasestorage.googleapis.com/v0/b/${bucket}/o/${encodedObjectName}?alt=media&token=${downloadToken}`,
    contentType
  };
}

function buildRemoteAssetRequest(remoteUrl, workoutBaseUrl, workoutApiKey) {
  if (!remoteUrl) {
    return { url: remoteUrl, headers: {} };
  }

  try {
    const url = new URL(remoteUrl);
    const workoutOrigin = new URL(workoutBaseUrl).origin;
    if (url.origin === workoutOrigin && workoutApiKey) {
      url.searchParams.set("api-key", workoutApiKey);
      return {
        url: url.toString(),
        headers: {
          "X-WorkoutX-Key": workoutApiKey
        }
      };
    }
  } catch {
    return { url: remoteUrl, headers: {} };
  }

  return { url: remoteUrl, headers: {} };
}

function createWorkoutXGate(minIntervalMs) {
  let lastRequestAt = 0;
  return {
    async waitTurn() {
      const now = Date.now();
      const waitMs = Math.max(0, lastRequestAt + minIntervalMs - now);
      if (waitMs > 0) {
        await sleep(waitMs);
      }
      lastRequestAt = Date.now();
    }
  };
}

async function deleteAllStorageObjects({ accessToken, bucket }) {
  let pageToken = "";
  do {
    const url = new URL(`${STORAGE_OBJECT_BASE}/b/${bucket}/o`);
    url.searchParams.set("maxResults", "1000");
    if (pageToken) url.searchParams.set("pageToken", pageToken);
    const response = await fetch(url, {
      headers: {
        Authorization: `Bearer ${accessToken}`
      }
    });
    if (!response.ok) {
      throw new Error(`Failed to list storage objects: ${response.status} ${await response.text()}`);
    }
    const payload = await response.json();
    for (const item of payload.items || []) {
      const encodedName = encodeURIComponent(item.name);
      const deleteResponse = await fetch(`${STORAGE_OBJECT_BASE}/b/${bucket}/o/${encodedName}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${accessToken}`
        }
      });
      if (!deleteResponse.ok && deleteResponse.status !== 404) {
        throw new Error(`Failed to delete ${item.name}: ${deleteResponse.status} ${await deleteResponse.text()}`);
      }
    }
    pageToken = payload.nextPageToken || "";
  } while (pageToken);
}

async function fetchWorkoutX(url, { headers, workoutGate }) {
  for (;;) {
    let response;
    try {
      if (workoutGate) {
        await workoutGate.waitTurn();
      }
      response = await fetch(url, { headers });
    } catch (error) {
      console.warn(`WorkoutX network error: ${error.message}. Retrying in 5000 ms.`);
      await sleep(5000);
      continue;
    }
    if (response.status !== 429) {
      return response;
    }
    const payload = await response.json().catch(() => ({}));
    const resetAt = Date.parse(payload.resetAt || "");
    const waitMs = Number.isFinite(resetAt)
      ? Math.max(1000, resetAt - Date.now() + 1000)
      : 60_000;
    console.warn(`WorkoutX rate limit hit. Waiting ${waitMs} ms before retrying.`);
    await sleep(waitMs);
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function inferContentType(objectPath) {
  if (objectPath.endsWith(".gif")) return "image/gif";
  if (objectPath.endsWith(".png")) return "image/png";
  if (objectPath.endsWith(".webp")) return "image/webp";
  return "image/jpeg";
}

function buildExerciseFirestoreDocument({ exercise, fetchedAt, thumbnailUpload, gifUpload }) {
  const target = stringOrEmpty(exercise.target);
  const bodyPart = stringOrEmpty(exercise.bodyPart || exercise.muscleGroup);
  const gifVersion = numberOrZero(exercise.gifVersion);

  return {
    id: stringOrEmpty(exercise.id),
    name: stringOrEmpty(exercise.name),
    bodyPart,
    target,
    primaryMuscleGroup: bodyPart,
    muscleGroup: stringOrEmpty(exercise.muscleGroup || bodyPart),
    targetMuscles: normalizeStringList(exercise.targetMuscles, target),
    equipment: stringOrEmpty(exercise.equipment),
    difficulty: stringOrEmpty(exercise.difficulty),
    description: stringOrEmpty(exercise.description),
    instructions: stringOrEmpty(exercise.instructions || exercise.description),
    calories: numberOrNull(exercise.calories),
    duration: numberOrNull(exercise.duration),
    defaultRepsText: stringOrEmpty(exercise.defaultRepsText),
    defaultDurationSeconds: numberOrNull(exercise.defaultDurationSeconds),
    thumbnailUrl: thumbnailUpload.downloadUrl,
    thumbnailStoragePath: thumbnailUpload.storagePath,
    thumbnailMimeType: thumbnailUpload.contentType,
    gifUrl: gifUpload.downloadUrl || stringOrEmpty(exercise.gifUrl),
    gifStoragePath: gifUpload.storagePath,
    gifMimeType: gifUpload.contentType,
    gifVersion,
    videoUrl: stringOrEmpty(exercise.videoUrl),
    mediaUrl: gifUpload.downloadUrl || stringOrEmpty(exercise.gifUrl),
    mediaType: "gif",
    steps: normalizeStringList(exercise.steps),
    mistakes: normalizeStringList(exercise.mistakes),
    tips: normalizeStringList(exercise.tips),
    variations: normalizeStringList(exercise.variations),
    updatedAt: stringOrEmpty(exercise.updatedAt || fetchedAt),
    source: "workoutx",
    sourceFetchedAt: fetchedAt,
    sourcePayload: exercise
  };
}

async function upsertFirestoreDocument({ accessToken, projectId, collection, documentId, fields }) {
  const documentPath = `${FIRESTORE_BASE}/projects/${projectId}/databases/(default)/documents/${collection}/${documentId}`;
  const response = await fetch(documentPath, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      fields: toFirestoreFields(fields)
    })
  });

  if (!response.ok) {
    throw new Error(`Failed to upsert Firestore document ${collection}/${documentId}: ${response.status} ${await response.text()}`);
  }
}

function toFirestoreFields(value) {
  const fields = {};
  for (const [key, entry] of Object.entries(value)) {
    fields[key] = toFirestoreValue(entry);
  }
  return fields;
}

function toFirestoreValue(value) {
  if (value === null || value === undefined) return { nullValue: null };
  if (Array.isArray(value)) {
    return {
      arrayValue: {
        values: value.map((entry) => toFirestoreValue(entry))
      }
    };
  }
  if (typeof value === "string") return { stringValue: value };
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number") {
    return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  }
  if (typeof value === "object") {
    return {
      mapValue: {
        fields: toFirestoreFields(value)
      }
    };
  }
  return { stringValue: String(value) };
}

function normalizeStringList(value, fallback) {
  if (Array.isArray(value)) {
    return value.map((entry) => String(entry)).filter(Boolean);
  }
  if (fallback) return [String(fallback)];
  return [];
}

function stringOrEmpty(value) {
  return value == null ? "" : String(value);
}

function numberOrNull(value) {
  if (value == null || value === "") return null;
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : null;
}

function numberOrZero(value) {
  const numeric = numberOrNull(value);
  return numeric == null ? 0 : numeric;
}

function base64UrlEncode(value) {
  return Buffer.from(value).toString("base64url");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
