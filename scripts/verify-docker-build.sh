#!/usr/bin/env bash
set -euo pipefail

IMAGE_NAME="${IMAGE_NAME:-bidmart-api-gateway:local-test}"

if [[ ! -f Dockerfile ]]; then
  echo "ERROR: Dockerfile is missing."
  exit 1
fi

docker build -t "$IMAGE_NAME" .

echo "OK: Docker image built as $IMAGE_NAME"
