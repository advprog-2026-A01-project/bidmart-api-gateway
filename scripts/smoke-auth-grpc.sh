#!/usr/bin/env bash
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"

if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq belum terinstall. Jalankan: sudo apt install -y jq"
  exit 1
fi

echo "==> Auth gRPC status through API Gateway"
curl -fsS "$GATEWAY_URL/api/gateway/internal/auth-grpc/status" | jq

echo "==> OK: API Gateway can reach Auth Service through gRPC"
