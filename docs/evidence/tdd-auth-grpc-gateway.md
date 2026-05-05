# TDD Evidence: API Gateway to Auth Service gRPC

## Scope

This evidence covers the internal gRPC communication from `bidmart-api-gateway` to `bidmart-auth-service`.

External communication remains REST/HTTP:

```text
Frontend -> API Gateway: REST/HTTP/JSON
API Gateway -> Auth Service: gRPC
```

## Red-Green-Refactor Commits

```text
83a32a2 [REFACTOR] Centralize auth grpc client properties
5844895 [GREEN] add auth grpc client and diagnostics endpoint
7c6a893 [RED] Add failing auth grpc diagnostics test
ed941b5 Merge pull request #1 from advprog-2026-A01-project/feat/internal-grpc-auth-client
a16bb5f feat: add internal grpc client for auth service
79c258e Added README.md
```

## Validation Commands

```bash
./gradlew clean generateProto test pmdMain pmdTest
./scripts/smoke-auth-grpc.sh
```

## Diagnostic Endpoint

```text
GET /api/gateway/internal/auth-grpc/status
```

This endpoint is a development/demo diagnostic endpoint to prove internal gRPC connectivity.
It is not a user-facing business feature.
