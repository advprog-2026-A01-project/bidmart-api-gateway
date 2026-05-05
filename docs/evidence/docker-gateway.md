# Docker Evidence: API Gateway

## Purpose

This document records Docker evidence for `bidmart-api-gateway`.

The API Gateway is the single public backend entry point for BidMart.
Frontend clients call the Gateway through REST/HTTP, while the Gateway may use internal service-to-service communication such as gRPC.

## Build Command

~~~bash
./scripts/verify-docker-build.sh
~~~

## Runtime Port

~~~text
8080
~~~

## Required Runtime Environment Variables

~~~text
AUTH_SERVICE_BASE_URL
JWT_JWK_SET_URI
AUTH_GRPC_HOST
AUTH_GRPC_PORT
FRONTEND_ORIGIN
GATEWAY_SECRET
~~~

## Local Example

~~~bash
docker run --rm -p 8080:8080 \
  -e AUTH_SERVICE_BASE_URL=http://host.docker.internal:8081 \
  -e JWT_JWK_SET_URI=http://host.docker.internal:8081/.well-known/jwks.json \
  -e AUTH_GRPC_HOST=host.docker.internal \
  -e AUTH_GRPC_PORT=9091 \
  bidmart-api-gateway:local-test
~~~

In Docker Compose, `host.docker.internal` will be replaced by Docker service names such as `bidmart-auth-service`.
