# Deployment & DevOps

Log of how the app is containerized, orchestrated, and built in CI. Seeds the formal report.

## Overview

Three services:

| Service    | Tech                    | Port (host) | Role |
|------------|-------------------------|-------------|------|
| `frontend` | Angular 22 + nginx      | `8081`      | Serves the built SPA and reverse-proxies `/api` → backend |
| `backend`  | Kotlin / Spring Boot 4.1 (Java 21) | internal `8080` | REST API, Flyway migrations, business logic |
| `db`       | PostgreSQL 18 (alpine)  | internal `5432` | Data store |

Request flow: **browser → frontend nginx** → static files, and any `/api/*` call is proxied to
**backend:8080** → **Postgres (db:5432)**. Single origin, so no CORS is needed.

## 1. Dockerization

### Backend — `backend/expense_tracker_backend/Dockerfile` (multi-stage)
- **Build stage** (`eclipse-temurin:21-jdk`): copies the Gradle wrapper + build scripts first (layer
  caching), then `src/`, and runs `./gradlew clean bootJar -x test`. Tests are skipped here because
  they use Testcontainers (Docker-in-Docker); they run in CI instead.
- **Runtime stage** (`eclipse-temurin:21-jre`): copies only the executable boot jar
  (`*-SNAPSHOT.jar`, excluding `*-plain.jar`), runs as a **non-root** user, exposes `8080`.
- Build/run standalone:
  ```bash
  docker build -t expense-tracker-backend ./backend/expense_tracker_backend
  docker run --rm -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... expense-tracker-backend
  ```

### Frontend — `frontend/Dockerfile` (multi-stage) + `frontend/nginx.conf`
- **Build stage** (`node:22-alpine`): `npm ci` then `npm run build` → `dist/frontend/browser`.
- **Runtime stage** (`nginx:alpine`): copies the bundle to `/usr/share/nginx/html` and our
  `nginx.conf`, which does SPA fallback (`try_files … /index.html`) and proxies `location /api/`
  to `http://backend:8080`.
- `.dockerignore` files keep `node_modules/`, `build/`, `dist/`, `.idea/` out of the build context.

## 2. Docker Compose — `docker-compose.yml`

Orchestrates all three services on one network:
- **db**: `postgres:18-alpine`, credentials from env, data on the named volume `pgdata`, with a
  `pg_isready` **healthcheck**.
- **backend**: built from its Dockerfile; `depends_on: db (condition: service_healthy)` so it only
  starts once the DB accepts connections; reaches the DB at `db:5432` via
  `DB_URL=jdbc:postgresql://db:5432/…`.
- **frontend**: built from its Dockerfile; `depends_on: backend`; published on host **8081**.

Run everything:
```bash
cp .env.example .env      # first time only
docker compose up --build
```
Then open **http://localhost:8081** and log in with **demo@demo.com / demo1234** (Flyway seeds demo
data on first start). Stop with `docker compose down` (add `-v` to also wipe the DB volume).

## 3. Configuration & secrets

`backend/.../application.yml` reads everything from env vars with local-dev fallbacks
(`${VAR:default}`):

| Variable | Purpose | Compose value |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Datasource | `jdbc:postgresql://db:5432/…`, `postgres`, from `.env` |
| `JWT_SECRET` | Signs access tokens | from `.env` |
| `ENCRYPTION_KEY` | AES key for encrypting users' Gemini API keys | from `.env` |
| `GEMINI_API_KEY` | Optional AI; `no-key` disables it | from `.env` |

Real values live in a git-ignored **`.env`** (Compose auto-loads it). **`.env.example`** is the
committed template.

## 4. CI — GitHub Actions (`.github/workflows/ci.yml`)

Triggers on **push** and **pull_request** to `develop`/`main`. Three jobs:

- **backend**: JDK 21 (Temurin) + Gradle cache → `./gradlew ktlintCheck test bootJar`. Testcontainers
  spins up a throwaway Postgres using the runner's own Docker daemon — no service container needed.
- **frontend**: Node 22 + npm cache → `npm ci`, `npm run lint` (angular-eslint), `npm run format:check`
  (Prettier), `npm run build`.
- **images** (`needs: backend, frontend`, only on `push`): logs in to Docker Hub, then
  `docker/build-push-action` builds and pushes **two** images, each tagged `:latest` and
  `:<git-sha>`:
  - `DOCKERHUB_USERNAME/expense-tracker-backend`
  - `DOCKERHUB_USERNAME/expense-tracker-frontend`

### Required repository secrets
Settings → Secrets and variables → Actions → **New repository secret**:
- `DOCKERHUB_USERNAME` — your Docker Hub username
- `DOCKERHUB_TOKEN` — a Docker Hub **access token** (Account Settings → Security → New Access Token),
  not your account password

### Linters added ("syntax tests")
- **Kotlin**: `org.jlleitschuh.gradle.ktlint` plugin → `ktlintCheck` (config in `.editorconfig`).
  Auto-fix locally with `./gradlew ktlintFormat`.
- **Angular**: `angular-eslint` (`eslint.config.js`) → `npm run lint`; Prettier → `npm run format:check`
  / `npm run format`.

## How it fits together

```
git push (develop/main)
      │
      ▼
GitHub Actions ── backend job (ktlint + tests + jar)
               ── frontend job (eslint + prettier + build)
               └─ images job → build & push 2 images → Docker Hub
                                                          │
local:  docker compose up --build ── pulls/builds ────────┘
        frontend:8081 → /api → backend:8080 → db:5432
```

## Issues encountered & fixes (running log)

Kept as we go, so the report can mention what broke and why.

### 1. `npm ci` failed in Docker/CI — Linux-only deps missing from the lockfile
- **Symptom:** the frontend image build failed at `RUN npm ci` with
  `EUSAGE … package.json and package-lock.json … are in sync … Missing: @emnapi/runtime@… from lock file`.
  It worked fine locally with `npm ci`.
- **Cause:** `package-lock.json` was generated on **macOS (arm64)**, so it omitted **Linux-only**
  native optional dependencies (e.g. `@emnapi/runtime`). `npm ci` is strict and refuses to install
  when the lockfile doesn't match the platform — and both Docker **and** GitHub Actions runners are
  Linux, so CI would have failed the same way.
- **Fix:** regenerate the lockfile on Linux so it includes those entries, then commit it:
  ```bash
  docker run --rm -v "$PWD/frontend":/app -w /app node:22-alpine npm install --package-lock-only
  ```
  Take-away: whenever native deps change, refresh `package-lock.json` on Linux (or in CI) and commit it.

### 2. Postgres 18 container exited on first start — wrong volume mount path
- **Symptom:** `db` container exited (code 1); `depends_on` then failed the whole stack. Logs said
  *"in 18+, these Docker images store data in a major-version-specific directory … there appears to be
  PostgreSQL data in /var/lib/postgresql/data (unused mount/volume)."*
- **Cause:** the named volume was mounted at `/var/lib/postgresql/data`. **Postgres 18** changed the
  convention — the mount must be the parent `/var/lib/postgresql` (data lives in a versioned subdir).
- **Fix:** in `docker-compose.yml`, mount `pgdata:/var/lib/postgresql` (not `/data`), then recreate
  the volume: `docker compose down -v && docker compose up -d`.

### 3. Container name conflict on `docker compose up`
- **Symptom:** `Conflict. The container name "/expense-tracker-db" is already in use`.
- **Cause:** a leftover container from the earlier standalone `backend/…/docker-compose.yml` (which
  also fixed `container_name: expense-tracker-db`).
- **Fix:** `docker rm -f expense-tracker-db` (or `docker compose down` on the old file) before bringing
  the root stack up.

## Next: CD + Kubernetes (Azure) — TODO
- Kubernetes manifests: Deployment + ConfigMap/Secret, Service, Ingress (app); StatefulSet +
  ConfigMap/Secret (Postgres); a dedicated namespace.
- CD: extend the pipeline to deploy to **Azure** (student credit) after images are pushed.
