#!/usr/bin/env bash
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
BIDMART_LOGIN_USERNAME="${BIDMART_LOGIN_USERNAME:-admin}"
BIDMART_LOGIN_PASSWORD="${BIDMART_LOGIN_PASSWORD:-admin12345}"

if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq belum terinstall. Jalankan: sudo apt install -y jq"
  exit 1
fi

echo "==> Gateway health"
curl -sS "$GATEWAY_URL/actuator/health" | jq

echo "==> DB ping via Gateway"
curl -sS "$GATEWAY_URL/api/db/ping" | jq

echo "==> Issue captcha"
CAPTCHA_JSON="$(curl -sS "$GATEWAY_URL/api/auth/captcha")"
echo "$CAPTCHA_JSON" | jq

CAPTCHA_ID="$(echo "$CAPTCHA_JSON" | jq -r '.captchaId // empty')"
CAPTCHA_ANSWER="$(echo "$CAPTCHA_JSON" | jq -r '.devAnswer // empty')"

if [[ -z "$CAPTCHA_ID" || -z "$CAPTCHA_ANSWER" ]]; then
  echo "ERROR: captchaId/devAnswer kosong."
  exit 1
fi

echo "==> Login via Gateway"
echo "Using username: $BIDMART_LOGIN_USERNAME"

LOGIN_BODY="$(jq -n \
  --arg username "$BIDMART_LOGIN_USERNAME" \
  --arg password "$BIDMART_LOGIN_PASSWORD" \
  --arg captchaId "$CAPTCHA_ID" \
  --arg captchaAnswer "$CAPTCHA_ANSWER" \
  '{
    username: $username,
    password: $password,
    captchaId: $captchaId,
    captchaAnswer: $captchaAnswer
  }')"

LOGIN_RESPONSE_FILE="$(mktemp)"

LOGIN_STATUS="$(curl -sS -o "$LOGIN_RESPONSE_FILE" -w "%{http_code}" \
  -X POST "$GATEWAY_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "$LOGIN_BODY")"

echo "HTTP $LOGIN_STATUS"
cat "$LOGIN_RESPONSE_FILE" | jq || cat "$LOGIN_RESPONSE_FILE"

if [[ "$LOGIN_STATUS" != "200" ]]; then
  echo "ERROR: login gagal."
  echo "DEBUG: login username yang dipakai adalah: $BIDMART_LOGIN_USERNAME"
  rm -f "$LOGIN_RESPONSE_FILE"
  exit 1
fi

ACCESS_TOKEN="$(jq -r '.accessToken // empty' "$LOGIN_RESPONSE_FILE")"
rm -f "$LOGIN_RESPONSE_FILE"

if [[ -z "$ACCESS_TOKEN" || "$ACCESS_TOKEN" == "null" ]]; then
  echo "ERROR: accessToken kosong."
  exit 1
fi

echo "==> Access token received"
echo "$ACCESS_TOKEN" | cut -c1-40
echo

echo "==> Protected endpoint via Gateway"
ME_RESPONSE_FILE="$(mktemp)"

ME_STATUS="$(curl -sS -o "$ME_RESPONSE_FILE" -w "%{http_code}" \
  "$GATEWAY_URL/api/auth/me" \
  -H "Authorization: Bearer $ACCESS_TOKEN")"

echo "HTTP $ME_STATUS"
cat "$ME_RESPONSE_FILE" | jq || cat "$ME_RESPONSE_FILE"

if [[ "$ME_STATUS" != "200" ]]; then
  echo "WARN: /api/auth/me gagal. Coba /api/users/me/profile ..."

  PROFILE_STATUS="$(curl -sS -o "$ME_RESPONSE_FILE" -w "%{http_code}" \
    "$GATEWAY_URL/api/users/me/profile" \
    -H "Authorization: Bearer $ACCESS_TOKEN")"

  echo "HTTP $PROFILE_STATUS"
  cat "$ME_RESPONSE_FILE" | jq || cat "$ME_RESPONSE_FILE"

  if [[ "$PROFILE_STATUS" != "200" ]]; then
    echo "ERROR: protected endpoint gagal."
    rm -f "$ME_RESPONSE_FILE"
    exit 1
  fi
fi

rm -f "$ME_RESPONSE_FILE"

echo "==> OK: Auth Service + API Gateway integration passed"
