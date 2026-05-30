import { createSign } from "node:crypto";
import { readFileSync, existsSync } from "node:fs";

const ROOT_ENV_PATH = ".env";
const GOOGLE_OAUTH_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

async function main() {
  const options = parseArgs(process.argv.slice(2));
  const env = loadDotEnv(ROOT_ENV_PATH);
  const serviceAccountPath = options.serviceAccountPath || requiredEnv(env, "FIREBASE_SERVICE_ACCOUNT_PATH");
  if (!existsSync(serviceAccountPath)) {
    throw new Error(`Service account file not found: ${serviceAccountPath}`);
  }

  const serviceAccount = JSON.parse(readFileSync(serviceAccountPath, "utf8"));
  const projectId = options.projectId || env.FIREBASE_PROJECT_ID || serviceAccount.project_id;
  const deviceToken = options.token || process.env.FCM_DEVICE_TOKEN;
  if (!deviceToken) {
    throw new Error("Missing device token. Pass --token <FCM token> or set FCM_DEVICE_TOKEN.");
  }

  const title = options.title || "Fitty test notification";
  const body = options.body || "This is a Firebase push test from the local script.";
  const route = options.route || "";

  const accessToken = await fetchGoogleAccessToken(serviceAccount);
  const response = await fetch(
    `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        message: {
          token: deviceToken,
          notification: {
            title,
            body
          },
          data: {
            title,
            body,
            route
          },
          android: {
            priority: "high",
            notification: {
              channel_id: "fitty_alerts_v2",
              sound: "default"
            }
          }
        }
      })
    }
  );

  const responseText = await response.text();
  if (!response.ok) {
    throw new Error(`FCM request failed (${response.status}): ${responseText}`);
  }

  console.log("FCM push sent successfully.");
  console.log(responseText);
}

function parseArgs(args) {
  const options = {};
  for (let index = 0; index < args.length; index += 1) {
    const argument = args[index];
    switch (argument) {
      case "--token":
        options.token = args[++index];
        break;
      case "--title":
        options.title = args[++index];
        break;
      case "--body":
        options.body = args[++index];
        break;
      case "--route":
        options.route = args[++index];
        break;
      case "--project-id":
        options.projectId = args[++index];
        break;
      case "--service-account":
        options.serviceAccountPath = args[++index];
        break;
      case "--help":
        printUsage();
        process.exit(0);
        break;
      default:
        throw new Error(`Unknown argument: ${argument}`);
    }
  }
  return options;
}

function printUsage() {
  console.log(`Usage:
  node scripts/send_fcm_test.mjs --token <fcm_device_token> [options]

Options:
  --title <text>             Notification title
  --body <text>              Notification body
  --route <value>            Extra data payload route
  --project-id <projectId>   Override Firebase project id
  --service-account <path>   Override service account JSON path
  --help                     Show this help

Environment:
  FCM_DEVICE_TOKEN           Alternative to --token
`);
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
  const nowSeconds = await resolveGoogleNowSeconds(serviceAccount.token_uri || GOOGLE_TOKEN_URL);
  const jwt = createJwt({
    clientEmail: serviceAccount.client_email,
    privateKey: serviceAccount.private_key,
    scope: GOOGLE_OAUTH_SCOPE,
    tokenUri: serviceAccount.token_uri || GOOGLE_TOKEN_URL,
    issuedAt: nowSeconds,
    expiresAt: nowSeconds + 3600
  });

  const body = new URLSearchParams({
    grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
    assertion: jwt
  });
  const response = await fetch(serviceAccount.token_uri || GOOGLE_TOKEN_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded"
    },
    body
  });
  const payload = await response.text();
  if (!response.ok) {
    throw new Error(`Unable to fetch Google access token (${response.status}): ${payload}`);
  }
  const parsed = JSON.parse(payload);
  if (!parsed.access_token) {
    throw new Error("Google OAuth response did not contain access_token.");
  }
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
    // Fall back to local clock if the header request is unavailable.
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
