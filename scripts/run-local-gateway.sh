#!/usr/bin/env bash
set -euo pipefail

export AUTH_SERVICE_BASE_URL="${AUTH_SERVICE_BASE_URL:-http://localhost:8081}"
export CATALOG_SERVICE_BASE_URL="${CATALOG_SERVICE_BASE_URL:-http://localhost:8082}"
export AUCTION_WALLET_SERVICE_BASE_URL="${AUCTION_WALLET_SERVICE_BASE_URL:-http://localhost:8083}"
export JWT_JWK_SET_URI="${JWT_JWK_SET_URI:-http://localhost:8081/.well-known/jwks.json}"
export FRONTEND_ORIGIN="${FRONTEND_ORIGIN:-http://localhost:5173}"
export GATEWAY_SECRET="${GATEWAY_SECRET:-local-dev-gateway-secret}"

echo "Starting BidMart API Gateway"
echo "AUTH_SERVICE_BASE_URL=$AUTH_SERVICE_BASE_URL"
echo "JWT_JWK_SET_URI=$JWT_JWK_SET_URI"
echo "FRONTEND_ORIGIN=$FRONTEND_ORIGIN"

./gradlew bootRun
