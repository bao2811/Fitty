import { createSign } from "node:crypto";
import { existsSync, readFileSync } from "node:fs";

const ROOT_ENV_PATH = ".env";
const GOOGLE_OAUTH_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
const FIRESTORE_BASE = "https://firestore.googleapis.com/v1";

async function main() {
  const options = parseArgs(process.argv.slice(2));
  const env = loadDotEnv(ROOT_ENV_PATH);
  const serviceAccountPath = options.serviceAccountPath || requiredEnv(env, "FIREBASE_SERVICE_ACCOUNT_PATH");
  const seedPath = options.seedPath || "scripts/firebase_content_seed.json";
  if (!existsSync(serviceAccountPath)) {
    throw new Error(`Service account file not found: ${serviceAccountPath}`);
  }
  if (!existsSync(seedPath)) {
    throw new Error(`Seed file not found: ${seedPath}`);
  }

  const serviceAccount = JSON.parse(readFileSync(serviceAccountPath, "utf8"));
  const projectId = options.projectId || env.FIREBASE_PROJECT_ID || serviceAccount.project_id;
  const seed = JSON.parse(readFileSync(seedPath, "utf8"));
  const accessToken = await fetchGoogleAccessToken(serviceAccount);

  await seedAppContent({ accessToken, projectId, appContent: seed.app_content || {} });
  await seedStarterTemplates({ accessToken, projectId, templates: seed.starter_plan_templates || {} });

  console.log("Firebase content seed completed.");
}

function parseArgs(args) {
  const options = {};
  for (let index = 0; index < args.length; index += 1) {
    const argument = args[index];
    switch (argument) {
      case "--project-id":
        options.projectId = args[++index];
        break;
      case "--service-account":
        options.serviceAccountPath = args[++index];
        break;
      case "--seed":
        options.seedPath = args[++index];
        break;
      case "--help":
        printUsage();
        process.exit(0);
      default:
        throw new Error(`Unknown argument: ${argument}`);
    }
  }
  return options;
}

function printUsage() {
  console.log(`Usage:
  node scripts/seed_firebase_content.mjs [options]

Options:
  --project-id <projectId>   Override Firebase project id
  --service-account <path>   Override service account JSON path
  --seed <path>              Override content seed JSON path
  --help                     Show this help
`);
}

async function seedAppContent({ accessToken, projectId, appContent }) {
  for (const [documentId, payload] of Object.entries(appContent)) {
    const topLevelFields = { ...payload };
    delete topLevelFields.items;
    await upsertDocument({
      accessToken,
      projectId,
      segments: ["app_content", documentId],
      fields: topLevelFields
    });
    if (payload.items && typeof payload.items === "object") {
      for (const [itemId, itemPayload] of Object.entries(payload.items)) {
        await upsertDocument({
          accessToken,
          projectId,
          segments: ["app_content", documentId, "items", itemId],
          fields: itemPayload
        });
      }
    }
  }
}

async function seedStarterTemplates({ accessToken, projectId, templates }) {
  for (const [documentId, payload] of Object.entries(templates)) {
    await upsertDocument({
      accessToken,
      projectId,
      segments: ["starter_plan_templates", documentId],
      fields: payload
    });
  }
}

async function upsertDocument({ accessToken, projectId, segments, fields }) {
  const parentPath = segments.slice(0, -1).join("/");
  const documentId = segments.at(-1);
  const url = `${FIRESTORE_BASE}/projects/${projectId}/databases/(default)/documents/${parentPath}/${documentId}`;
  const response = await fetch(url, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      fields: encodeFirestoreMap(fields)
    })
  });
  if (!response.ok) {
    throw new Error(`Failed to seed ${segments.join("/")}: ${response.status} ${await response.text()}`);
  }
  console.log(`Seeded ${segments.join("/")}`);
}

function encodeFirestoreMap(value) {
  const result = {};
  for (const [key, entry] of Object.entries(value || {})) {
    result[key] = encodeFirestoreValue(entry);
  }
  return result;
}

function encodeFirestoreValue(value) {
  if (value === null || value === undefined) {
    return { nullValue: null };
  }
  if (Array.isArray(value)) {
    return {
      arrayValue: {
        values: value.map((entry) => encodeFirestoreValue(entry))
      }
    };
  }
  if (typeof value === "string") {
    return { stringValue: value };
  }
  if (typeof value === "boolean") {
    return { booleanValue: value };
  }
  if (typeof value === "number") {
    return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  }
  if (typeof value === "object") {
    return {
      mapValue: {
        fields: encodeFirestoreMap(value)
      }
    };
  }
  throw new Error(`Unsupported Firestore value type: ${typeof value}`);
}

function loadDotEnv(filePath) {
  if (!existsSync(filePath)) return {};
  return readFileSync(filePath, "utf8")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith("#") && line.includes("="))
    .reduce((accumulator, line) => {
      const separatorIndex = line.indexOf("=");
      const key = line.slice(0, separatorIndex).trim();
      const value = line.slice(separatorIndex + 1).trim().replace(/^"(.*)"$/, "$1");
      accumulator[key] = value;
      return accumulator;
    }, {});
}

function requiredEnv(env, key) {
  const value = env[key] || process.env[key];
  if (!value) {
    throw new Error(`Missing required environment variable ${key}`);
  }
  return value;
}

async function fetchGoogleAccessToken(serviceAccount) {
  const nowSeconds = await resolveGoogleNowSeconds(serviceAccount.token_uri);
  const issuedAt = nowSeconds - 120;
  const jwt = createJwt({
    clientEmail: serviceAccount.client_email,
    privateKey: serviceAccount.private_key,
    scope: GOOGLE_OAUTH_SCOPE,
    tokenUri: serviceAccount.token_uri,
    issuedAt,
    expiresAt: issuedAt + 3500
  });

  const response = await fetch(serviceAccount.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt
    })
  });
  const payload = await response.text();
  if (!response.ok) {
    throw new Error(`Unable to fetch Google access token (${response.status}): ${payload}`);
  }
  const parsed = JSON.parse(payload);
  return parsed.access_token;
}

async function resolveGoogleNowSeconds(tokenUri) {
  try {
    const response = await fetch(tokenUri, { method: "HEAD" });
    const dateHeader = response.headers.get("date");
    if (dateHeader) {
      const parsed = Date.parse(dateHeader);
      if (!Number.isNaN(parsed)) {
        return Math.floor(parsed / 1000);
      }
    }
  } catch {
    // fall back to local clock
  }
  return Math.floor(Date.now() / 1000);
}

function createJwt({ clientEmail, privateKey, scope, tokenUri, issuedAt, expiresAt }) {
  const header = { alg: "RS256", typ: "JWT" };
  const payload = {
    iss: clientEmail,
    sub: clientEmail,
    scope,
    aud: tokenUri,
    iat: issuedAt,
    exp: expiresAt
  };
  const encodedHeader = base64UrlEncode(JSON.stringify(header));
  const encodedPayload = base64UrlEncode(JSON.stringify(payload));
  const signingInput = `${encodedHeader}.${encodedPayload}`;
  const signer = createSign("RSA-SHA256");
  signer.update(signingInput);
  signer.end();
  const signature = signer.sign(privateKey);
  return `${signingInput}.${base64UrlEncode(signature)}`;
}

function base64UrlEncode(input) {
  const buffer = Buffer.isBuffer(input) ? input : Buffer.from(input);
  return buffer.toString("base64").replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
});
