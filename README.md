# BidMart API Gateway

`bidmart-api-gateway` adalah single public backend entry point untuk arsitektur microservices BidMart.

Frontend **tidak boleh** memanggil backend service langsung. Semua request dari frontend harus masuk melalui API Gateway.

Alur utama:

```text
Frontend
   ↓
bidmart-api-gateway
   ↓
Auth Service / Catalog Service / Auction-Wallet Service
```

---

# Role dalam Arsitektur BidMart

API Gateway bertanggung jawab untuk:

- menjadi satu-satunya public entry point backend;
- routing request frontend ke service internal;
- validasi JWT access token menggunakan JWKS dari Auth Service;
- centralize CORS configuration;
- membersihkan header user palsu dari client;
- inject trusted user context header ke downstream services;
- menyembunyikan internal service URL dari frontend.

---

# Service Mapping

Arsitektur backend BidMart:

```text
frontend-bidmart
   ↓
bidmart-api-gateway
   ├── bidmart-auth-service
   ├── bidmart-catalog-service
   └── bidmart-auction-wallet-service
```

Setiap service memiliki database sendiri:

```text
Auth Service           → auth_db
Catalog Service        → catalog_db
Auction-Wallet Service → auction_wallet_db
```

Tidak boleh ada foreign key antar database/service.

---

# Local Ports

| Component | Local Port |
|---|---:|
| API Gateway | `8080` |
| Auth Service | `8081` |
| Catalog Service | `8082` |
| Auction-Wallet Service | `8083` |
| Frontend | `5173` |
| Auth PostgreSQL | `5434` |

---

# Tech Stack

- Java 21
- Spring Boot
- Spring Cloud Gateway
- Spring Security OAuth2 Resource Server
- JWT validation using JWKS
- Gradle
- PMD

---

# Environment Variables

| Variable | Default Local | Keterangan |
|---|---|---|
| `PORT` | `8080` | Port API Gateway |
| `AUTH_SERVICE_BASE_URL` | `http://localhost:8081` | Base URL Auth Service |
| `CATALOG_SERVICE_BASE_URL` | `http://localhost:8082` | Base URL Catalog Service |
| `AUCTION_WALLET_SERVICE_BASE_URL` | `http://localhost:8083` | Base URL Auction-Wallet Service |
| `JWT_JWK_SET_URI` | `http://localhost:8081/.well-known/jwks.json` | JWKS endpoint dari Auth Service |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | Origin frontend untuk CORS |
| `GATEWAY_SECRET` | `local-dev-gateway-secret` | Secret internal Gateway untuk downstream services |

---

# Route Mapping

## Auth Service Routes

Request berikut diteruskan ke `AUTH_SERVICE_BASE_URL`:

```text
/api/auth/**
/api/admin/**
/api/rbac/**
/api/db/**
/api/users/**
```

Contoh:

```text
GET http://localhost:8080/api/auth/captcha
↓
GET http://localhost:8081/api/auth/captcha
```

```text
POST http://localhost:8080/api/auth/login
↓
POST http://localhost:8081/api/auth/login
```

## Catalog Service Routes

Request berikut diteruskan ke `CATALOG_SERVICE_BASE_URL`:

```text
/api/catalog/**
/api/categories/**
/api/listings/**
```

Contoh nantinya:

```text
GET http://localhost:8080/api/listings
↓
GET http://localhost:8082/api/listings
```

Jika Catalog Service belum berjalan, endpoint ini dapat menghasilkan error seperti `502 Bad Gateway`. Itu normal selama service tujuannya belum dijalankan.

## Auction-Wallet Service Routes

Request berikut diteruskan ke `AUCTION_WALLET_SERVICE_BASE_URL`:

```text
/api/auctions/**
/api/bids/**
/api/wallet/**
```

Contoh nantinya:

```text
POST http://localhost:8080/api/bids
↓
POST http://localhost:8083/api/bids
```

Jika Auction-Wallet Service belum berjalan, endpoint ini dapat menghasilkan error seperti `502 Bad Gateway`. Itu normal selama service tujuannya belum dijalankan.

---

# Authentication Model

Auth Service menerbitkan JWT access token dan expose public key melalui:

```text
GET /.well-known/jwks.json
```

API Gateway membaca public key tersebut dari:

```text
JWT_JWK_SET_URI=http://localhost:8081/.well-known/jwks.json
```

Lalu API Gateway memvalidasi JWT secara lokal.

Artinya:

- Gateway tidak perlu introspection ke Auth Service untuk setiap request;
- Auth Service tidak menjadi bottleneck untuk semua request;
- access token dapat divalidasi cepat oleh Gateway;
- downstream services tidak perlu validate JWT sendiri jika semua traffic wajib lewat Gateway.

---

# Public Endpoint dan Protected Endpoint

Beberapa endpoint boleh diakses tanpa JWT, misalnya:

```text
GET  /actuator/health
GET  /api/db/ping
GET  /api/auth/captcha
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/verify-email
POST /api/auth/2fa/verify
GET  /api/users/*/public-profile
```

Endpoint lain membutuhkan header:

```text
Authorization: Bearer <access_token>
```

Contoh:

```bash
curl http://localhost:8080/api/users/me/profile \
  -H "Authorization: Bearer <access_token>"
```

---

# Trusted User Context Headers

Client tidak boleh dipercaya untuk mengirim user context sendiri.

API Gateway akan menghapus header berikut dari request client:

```text
X-User-Id
X-Username
X-User-Role
X-User-Permissions
X-Gateway-Secret
```

Setelah JWT valid, API Gateway akan inject ulang header yang trusted:

```text
X-User-Id
X-Username
X-User-Role
X-User-Permissions
X-Gateway-Secret
```

Downstream service seperti Catalog Service dan Auction-Wallet Service harus membaca user identity dari header ini, bukan dari request body.

Contoh:

```text
X-User-Id: 1
X-Username: admin
X-User-Role: ADMIN
X-User-Permissions: users:read,users:write,wallet:read
X-Gateway-Secret: local-dev-gateway-secret
```

---

# Kontrak untuk Downstream Service

Nantinya `bidmart-catalog-service` dan `bidmart-auction-wallet-service` harus mengikuti kontrak ini:

1. Service hanya menerima request dari API Gateway.
2. Service mengecek `X-Gateway-Secret`.
3. Service membaca user identity dari:
   ```text
   X-User-Id
   X-Username
   X-User-Role
   X-User-Permissions
   ```
4. Service tidak melakukan query langsung ke `auth_db`.
5. Service tidak membuat foreign key ke tabel Auth Service.
6. Service menyimpan external id saja, misalnya:
   ```text
   seller_id
   bidder_id
   user_id
   ```

Contoh untuk Catalog Service:

```text
listing.seller_id = X-User-Id
```

Contoh untuk Auction-Wallet Service:

```text
bid.bidder_id = X-User-Id
wallet.user_id = X-User-Id
```

---

# Run Locally

## Prerequisites

Pastikan sudah ada:

- Java 21
- Docker
- jq
- Auth Service berjalan di port `8081`

Install `jq` jika belum ada:

```bash
sudo apt update
sudo apt install -y jq
```

---

# Local Run Order

## Terminal 1 — Start Auth Database

Dijalankan dari repo `bidmart-auth-service` atau folder mana saja:

```bash
docker rm -f bidmart-auth-db 2>/dev/null || true

docker run --name bidmart-auth-db \
  -e POSTGRES_DB=auth_db \
  -e POSTGRES_USER=auth \
  -e POSTGRES_PASSWORD=auth \
  -p 5434:5432 \
  -d postgres:16
```

## Terminal 2 — Start Auth Service

```bash
cd ../bidmart-auth-service
./scripts/run-local-auth.sh
```

Auth Service harus hidup di:

```text
http://localhost:8081
```

Cek JWKS:

```bash
curl -s http://localhost:8081/.well-known/jwks.json | jq
```

## Terminal 3 — Start API Gateway

```bash
cd ../bidmart-api-gateway
./scripts/run-local-gateway.sh
```

API Gateway harus hidup di:

```text
http://localhost:8080
```

## Terminal 4 — Smoke Test

```bash
cd ../bidmart-api-gateway
./scripts/smoke-auth-gateway.sh
```

Expected akhir:

```text
==> OK: Auth Service + API Gateway integration passed
```

---

# Run Script

API Gateway dapat dijalankan dengan:

```bash
./scripts/run-local-gateway.sh
```

Script ini akan memakai default:

```text
AUTH_SERVICE_BASE_URL=http://localhost:8081
CATALOG_SERVICE_BASE_URL=http://localhost:8082
AUCTION_WALLET_SERVICE_BASE_URL=http://localhost:8083
JWT_JWK_SET_URI=http://localhost:8081/.well-known/jwks.json
FRONTEND_ORIGIN=http://localhost:5173
GATEWAY_SECRET=local-dev-gateway-secret
```

Override contoh:

```bash
AUTH_SERVICE_BASE_URL=http://localhost:8081 \
FRONTEND_ORIGIN=http://localhost:5173 \
./scripts/run-local-gateway.sh
```

---

# Smoke Test

Smoke test mengecek:

1. Gateway health;
2. DB ping via Gateway;
3. issue captcha via Gateway;
4. login admin via Gateway;
5. access protected endpoint via Gateway.

Jalankan:

```bash
./scripts/smoke-auth-gateway.sh
```

Default credential:

```text
username: admin
password: admin12345
```

Override credential:

```bash
BIDMART_LOGIN_USERNAME=admin \
BIDMART_LOGIN_PASSWORD=admin12345 \
./scripts/smoke-auth-gateway.sh
```

Catatan: jangan memakai env variable `USERNAME`, karena di Linux nilainya bisa berisi username OS seperti `neal-guarddin`.

---

# Manual Smoke Test

## Health Check

```bash
curl -s http://localhost:8080/actuator/health | jq
```

Expected:

```json
{
  "status": "UP"
}
```

## DB Ping via Gateway

```bash
curl -s http://localhost:8080/api/db/ping | jq
```

Expected:

```json
{
  "db": 1
}
```

## Captcha

```bash
CAPTCHA_JSON=$(curl -s http://localhost:8080/api/auth/captcha)
echo "$CAPTCHA_JSON" | jq

CAPTCHA_ID=$(echo "$CAPTCHA_JSON" | jq -r '.captchaId')
CAPTCHA_ANSWER=$(echo "$CAPTCHA_JSON" | jq -r '.devAnswer')
```

## Login

```bash
LOGIN_JSON=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{
    \"username\":\"admin\",
    \"password\":\"admin12345\",
    \"captchaId\":\"$CAPTCHA_ID\",
    \"captchaAnswer\":\"$CAPTCHA_ANSWER\"
  }")

echo "$LOGIN_JSON" | jq

ACCESS_TOKEN=$(echo "$LOGIN_JSON" | jq -r '.accessToken')
```

## Protected Endpoint

```bash
curl -s http://localhost:8080/api/users/me/profile \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq
```

---

# Test and PMD

Run unit test:

```bash
./gradlew clean test
```

Run PMD:

```bash
./gradlew pmdMain pmdTest
```

Expected:

```text
BUILD SUCCESSFUL
```

---

# Integrasi dengan Frontend

Frontend harus diarahkan ke API Gateway, bukan ke Auth Service langsung.

Di repo `frontend-bidmart`, buat atau update:

```text
.env.local
```

Isi:

```text
VITE_API_BASE_URL=http://localhost:8080
```

Frontend harus melakukan request ke:

```text
import.meta.env.VITE_API_BASE_URL
```

Contoh:

```text
POST ${VITE_API_BASE_URL}/api/auth/login
GET  ${VITE_API_BASE_URL}/api/users/me/profile
GET  ${VITE_API_BASE_URL}/api/listings
POST ${VITE_API_BASE_URL}/api/bids
```

Jangan hardcode:

```text
http://localhost:8081
http://localhost:8082
http://localhost:8083
```

---

# Integrasi dengan Service Baru

Jika nanti menambahkan service baru, lakukan langkah berikut:

1. Tentukan service base URL di `application.yml`.
2. Tambahkan environment variable baru jika perlu.
3. Tambahkan route baru di `RouteConfig`.
4. Pastikan frontend tetap call Gateway.
5. Pastikan service baru membaca user context dari header Gateway.
6. Pastikan service baru tidak mengakses database service lain.

Contoh route tambahan:

```text
/api/notifications/**
```

Nantinya diarahkan ke:

```text
NOTIFICATION_SERVICE_BASE_URL
```

---

# Troubleshooting

## `502 Bad Gateway`

Service tujuan belum jalan.

Contoh:

```text
/api/listings → but Catalog Service belum hidup
```

Fix:

- jalankan service tujuan; atau
- untuk sementara abaikan jika service tersebut belum dibuat.

## Login berhasil, tapi protected endpoint 401

Kemungkinan Gateway masih membaca JWKS lama.

Fix:

1. Pastikan Auth Service memakai persistent `.local-keys`.
2. Restart API Gateway.
3. Jalankan smoke test ulang.

## `invalid_credentials`

Pastikan credential local admin:

```text
username: admin
password: admin12345
```

Jika smoke script gagal, pakai:

```bash
BIDMART_LOGIN_USERNAME=admin \
BIDMART_LOGIN_PASSWORD=admin12345 \
./scripts/smoke-auth-gateway.sh
```

## Gateway tidak bisa start karena port 8080 dipakai

Cari dan kill process:

```bash
lsof -ti tcp:8080 | xargs -r kill
```

Lalu run ulang:

```bash
./scripts/run-local-gateway.sh
```

## Auth Service tidak bisa start karena port 8081 dipakai

```bash
lsof -ti tcp:8081 | xargs -r kill
```

Lalu run ulang Auth Service.

---

# Current Local Integration Status

Integration yang sudah divalidasi:

```text
Frontend target URL: http://localhost:8080
API Gateway:        http://localhost:8080
Auth Service:       http://localhost:8081
Auth DB:            localhost:5434/auth_db
JWKS:               http://localhost:8081/.well-known/jwks.json
```

Smoke test membuktikan:

```text
Gateway health ✅
DB ping via Gateway ✅
Captcha via Gateway ✅
Login via Gateway ✅
Protected endpoint via Gateway ✅
```
