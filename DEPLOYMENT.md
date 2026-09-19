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

## 5. Kubernetes (local, k3d)

Manifests live in `k8s/` (applied with Kustomize). All objects go into the **`expense-tracker`**
namespace.

| File | Object | Notes |
|---|---|---|
| `namespace.yaml` | Namespace `expense-tracker` | everything is scoped here |
| `db-configmap.yaml` / `db-secret.yaml` | ConfigMap `db-config` / Secret `db-secret` | `POSTGRES_DB/USER` (config), `POSTGRES_PASSWORD` (secret) |
| `db-service.yaml` | headless Service `db` | `clusterIP: None`, backend reaches it at `db:5432` |
| `db-statefulset.yaml` | StatefulSet `db` (1 replica) | `postgres:18-alpine`, PVC `volumeClaimTemplates` mounted at **`/var/lib/postgresql`** (PG18), `pg_isready` probes |
| `backend-configmap.yaml` / `backend-secret.yaml` | ConfigMap `backend-config` / Secret `backend-secret` | `DB_URL`,`DB_USERNAME` (config); `DB_PASSWORD`,`JWT_SECRET`,`ENCRYPTION_KEY`,`GEMINI_API_KEY` (secret) |
| `backend-deployment.yaml` | Deployment `backend` (1 replica) | Docker Hub image; `initContainer` waits for `db:5432`; TCP probes on 8080 |
| `backend-service.yaml` | Service `backend` (8080) | **must be named `backend`** — frontend nginx proxies `/api` here |
| `frontend-deployment.yaml` | Deployment `frontend` (3 replicas) | stateless nginx, HTTP probes on `/` |
| `frontend-service.yaml` | Service `frontend` (80) | |
| `ingress.yaml` | Ingress (traefik) | host `expense.localhost`, `/` → `frontend:80` (nginx handles `/api`) |
| `kustomization.yaml` | Kustomize entry | sets namespace + **your Docker Hub username** via `images:` |

**Scaling rationale (why these replica counts):** per-user data is rows in one shared Postgres and
auth is stateless JWT, so the **frontend scales freely (3 replicas)** — Traefik load-balances across
them and one pod dying doesn't take the site down. The **backend stays at 1** because it runs the
`@Scheduled` summary cron — running 2+ would double-fire it (see below). The DB is a single
**StatefulSet** with a persistent PVC.

### Scheduled jobs (`@Scheduled`) when scaling the backend
Spring's `@Scheduled` fires **once per running instance**. With one backend replica that's fine, but
if we scale the backend to N replicas the summary cron would run **N times** at each trigger —
duplicate summaries, wasted work, possible race conditions. So we deliberately keep `backend`
`replicas: 1`. To scale the backend horizontally in production we'd make the schedule run on **exactly
one instance** using one of:
- **ShedLock** (`net.javacrumbs.shedlock`) — the simplest: wrap the job with `@SchedulerLock`; it takes
  a short-lived lock row in Postgres, so only the first replica to grab it runs that tick.
- **Leader election** — one pod is elected leader (e.g. via a k8s `Lease`) and only it runs schedules.
- **Externalize the schedule** — move it out of the app to a Kubernetes `CronJob` that invokes an
  internal endpoint / job runner, so it's independent of replica count.

Recommended here: **ShedLock**, since we already have Postgres and it's a few lines. Until that's
added, `backend` must stay at a single replica.

### Run it on k3d
```bash
# 1. one-time: set your Docker Hub username in k8s/kustomization.yaml (images:)
# 2. cluster with Traefik's LB mapped to host :8080
k3d cluster create expense --port "8080:80@loadbalancer" --wait
# 3. deploy
kubectl apply -k k8s/
# 4. watch
kubectl -n expense-tracker rollout status deploy/backend deploy/frontend
kubectl -n expense-tracker get pods,svc,ingress,statefulset,pvc
# 5. reach it (add "127.0.0.1 expense.localhost" to /etc/hosts, or use curl -H)
curl -H "Host: expense.localhost" http://localhost:8080/
```
Then open **http://expense.localhost:8080**, log in `demo@demo.com / demo1234`.

- **Flyway dev seed data (deliberate choice):** the `db/migration/dev/` seed migrations (demo user
  `demo@demo.com`, mock transactions) live **inside** the `db/migration` tree, and `flyway.locations`
  scans that path recursively — so they run in the container and in k8s too, not only under the local
  `dev` profile. We **left this on intentionally** so the demo/cluster comes up with data to log in
  with and show. For a real production deploy we'd move the dev files out of `db/migration` and add
  that location only under the `dev` profile so prod starts empty.
- **Images**: the manifests pull from Docker Hub, so the CI `images` job must have pushed
  `<user>/expense-tracker-{backend,frontend}:latest` and the repos must be **public** (or add an
  `imagePullSecret`).
- **StatefulSet persistence demo**: `kubectl -n expense-tracker delete pod db-0` → it re-attaches the
  same PVC and data survives.
- **Local verification without Docker Hub** (what we used to prove the manifests): build images, then
  `k3d image import expense-tracker-backend:local expense-tracker-frontend:local -c expense`, and apply
  with the image refs swapped to `:local`.
- **Teardown**: `k3d cluster delete expense`.

### How images are fetched & how updates roll out
- **Source = Docker Hub.** Each Deployment/StatefulSet references an image; `kustomization.yaml`
  rewrites those to `<user>/expense-tracker-{backend,frontend}:<tag>`. On pod start the kubelet pulls
  that image from Docker Hub (public repos, so no pull secret). It's the **same registry** the CI
  `images` job pushes to — CI publishes, k8s consumes.
- **How a rolling update happens.** Kubernetes only re-pulls / re-rolls when the **pod spec changes**,
  which in practice means the **image tag changes**. Deployments use a `RollingUpdate` strategy by
  default: it starts new pods, waits for their readiness probes, then removes old ones — zero downtime.
  Two ways to trigger it:
  - **Immutable tags (recommended):** tag images with the git SHA (CI already pushes `:<git-sha>`), and
    have the deploy step set that tag (`kustomize edit set image …=<user>/…:<sha>` or
    `kubectl set image`). Every commit → new tag → guaranteed rollout, and you can roll back to an exact
    SHA. This is the piece the future **CD job** will automate (build+push, then apply with the new SHA).
  - **`:latest`:** `:latest` alone does **not** auto-update running pods — the tag string didn't change,
    so k8s sees no diff. You'd force it with `kubectl -n expense-tracker rollout restart deploy/backend`
    (re-pulls if `imagePullPolicy: Always`). "New image appears → auto rolling update" is **not** a
    built-in k8s feature with a fixed tag; tools like **Argo CD**, **Flux**, or **Keel** add that
    (they watch the registry / git and apply changes). For this project the CD job pushing an
    SHA-tagged image and running `kubectl apply` is the simpler, standard approach.
- **Swapping the backend or the DB when there are changes.**
  - **Backend:** identical to the frontend flow — new image tag → `kubectl apply -k k8s/` (or `set
    image`) → rolling update. Its `initContainer` re-checks the DB is up; if a change adds a Flyway
    migration, it runs automatically on the new pod's boot. Roll back with
    `kubectl -n expense-tracker rollout undo deploy/backend`.
  - **DB (StatefulSet):** more careful, because it's **stateful**. Bumping the Postgres image rolls the
    single `db-0` pod but the **PVC (data) is kept and re-attached** — so config/image changes are safe,
    but a **major Postgres version jump** needs a real data migration (dump/restore or `pg_upgrade`),
    not just a tag bump. StatefulSets update one ordinal at a time (`OrderedReady`); with one replica
    that's just `db-0`. Schema changes to *our* data go through **Flyway** (backend migrations), never by
    editing the DB image.

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

### 4. Backend crash-looped on first deploy to k8s (DB not ready)
- **Symptom:** `backend` pod restarted a few times before going Ready.
- **Cause:** Kubernetes has no `depends_on` ordering like Compose, so the backend booted before
  Postgres accepted connections; Flyway failed → the pod crashed and restarted until the DB was up.
- **Fix:** added an `initContainer` (`busybox` `until nc -z db 5432`) to `backend-deployment.yaml`
  that blocks startup until the DB is reachable → clean start, 0 restarts.

### 5. `ImagePullBackOff` on local k3d — CI images were amd64-only
- **Symptom:** on the Apple Silicon Mac, `backend` and `frontend` pods stuck in `ImagePullBackOff`;
  `describe pod` showed `no match for platform in manifest: not found`. The DB (`postgres`) and
  `busybox` init container pulled fine.
- **Cause:** the GitHub Actions runner is **linux/amd64**, so `docker/build-push-action` published
  **amd64-only** images. The k3d cluster runs in Docker on the **arm64** Mac, so the kubelet couldn't
  find a matching architecture. (`postgres`/`busybox` are official **multi-arch** images, hence fine.)
- **Fix (CI):** build **multi-arch** — added `docker/setup-qemu-action` and
  `platforms: linux/amd64,linux/arm64` to both build-push steps. The Docker Hub images now run on
  amd64 (CI / Azure AKS) **and** arm64 (local k3d), so `kubectl apply -k k8s/` works everywhere.
- **Local workaround (before the multi-arch images exist):** build natively and import, then use the
  local copy instead of pulling:
  ```bash
  docker build -t <user>/expense-tracker-backend:local ./backend/expense_tracker_backend
  docker build -t <user>/expense-tracker-frontend:local ./frontend
  k3d image import <user>/expense-tracker-backend:local <user>/expense-tracker-frontend:local -c expense
  kubectl -n expense-tracker set image deploy/backend backend=<user>/expense-tracker-backend:local
  kubectl -n expense-tracker set image deploy/frontend frontend=<user>/expense-tracker-frontend:local
  # a :latest tag defaults to imagePullPolicy: Always -> force it to use the imported copy:
  kubectl -n expense-tracker patch deploy backend  --type=json \
    -p='[{"op":"replace","path":"/spec/template/spec/containers/0/imagePullPolicy","value":"IfNotPresent"}]'
  kubectl -n expense-tracker patch deploy frontend --type=json \
    -p='[{"op":"replace","path":"/spec/template/spec/containers/0/imagePullPolicy","value":"IfNotPresent"}]'
  ```
  Take-away: on Apple Silicon always publish multi-arch images, or the local cluster can't pull them.

### 6. AKS create rejected — subscription region policy (Azure for Students)
- **Symptom:** `az aks create` in `westeurope` failed with `(RequestDisallowedByAzure) … This policy
  maintains a set of best available regions where your subscription can deploy resources.`
- **Cause:** the **Azure for Students** subscription has an *Allowed resource deployment regions*
  policy; `westeurope` isn't in it.
- **Fix:** discover the allowed regions and deploy into one of them:
  ```bash
  az policy assignment list -o json | python3 -c "import sys,json;[print(a['parameters']) for a in json.load(sys.stdin) if a.get('parameters') and 'listOfAllowedLocations' in a['parameters']]"
  ```
  Allowed here: `germanywestcentral, italynorth, spaincentral, austriaeast, belgiumcentral`. Recreated
  the resource group and cluster in **`germanywestcentral`**.

### 7. AKS create rejected — resource providers not registered
- **Symptom:** `(MissingSubscriptionRegistration) The subscription is not registered to use namespace
  'Microsoft.ContainerService'`.
- **Cause:** a brand-new subscription hasn't registered the AKS resource providers yet.
- **Fix:** register them once (async, ~1–3 min), wait, then retry:
  ```bash
  for ns in Microsoft.ContainerService Microsoft.Network Microsoft.Compute Microsoft.Storage Microsoft.OperationalInsights; do az provider register --namespace $ns; done
  az provider show -n Microsoft.ContainerService --query registrationState -o tsv   # wait for 'Registered'
  ```

### 8. AKS create rejected — VM size not allowed in the region
- **Symptom:** `(BadRequest) The VM size of Standard_B2s is not allowed in your subscription in
  location 'germanywestcentral'.` (the B-series burstable SKUs aren't offered to the student sub here).
- **Cause:** Azure for Students restricts both regions *and* VM SKUs; the allowed list for this region
  is D/E/F/L/M families (e.g. the smallest `standard_d2s_v7`, `standard_f2as_v6`).
- **Fix:** pick an allowed small SKU — used **`Standard_D2s_v7`** (2 vCPU / 8 GB). List what's allowed:
  `az vm list-skus -l germanywestcentral --size Standard_D2 --output table`, or read the SKU list Azure
  prints in the error.

### 9. No VM SKU both available *and* with quota in most allowed regions
- **Symptom:** after picking an allowed region, `az aks create` still failed — either the SKU was
  `NotAvailableForSubscription`, or `(ErrCode_InsufficientVCPUQuota) … remaining 0 for family …`.
  In `germanywestcentral`/`italynorth`/`spaincentral` the *intersection* of "SKU available to this
  subscription" and "family has vCPU quota > 0" was **empty**.
- **Cause:** Azure for Students limits both which SKUs each region offers *and* the per-family vCPU
  quota (mostly 0–4). The allowed-region policy and the quota grants don't line up in every region.
- **Fix:** script the intersection across the allowed regions — for each region, list VM SKUs with no
  `restrictions`, keep 2-vCPU ones, and check the family's remaining quota via `az vm list-usage`.
  Only **`austriaeast`** and **`belgiumcentral`** had a match: `Standard_DS2_v2_Promo`
  (family `standardDSv2PromoFamily`, quota 4). Deployed there.
- Note: the policy's "Not registered" *compliance state* in the portal is just the scan status — the
  allowed-regions parameter was already populated (confirmed via `az policy assignment list`), so this
  was a SKU/quota problem, not a wait-for-policy-provisioning one.

## 6. Azure (AKS) hosting + automated CD

The same manifests run on **Azure Kubernetes Service (AKS)**. AKS nodes are **amd64**, so the Docker
Hub images (`matejbangievski/expense-tracker-{backend,frontend}`) pull natively — no local
build/import as on the arm64 Mac. Only two things differ from k3d, so they live in a small **Kustomize
overlay** (`k8s-azure/`) that reuses the base `k8s/` and patches just the Ingress; the local k3d flow
(`kubectl apply -k k8s/`) is unchanged.

### Provision (one-time, `az`)
```bash
az account set --subscription "<student-subscription-id>"
RG=expense-rg; LOC=austriaeast; AKS=expense-aks
az group create -n $RG -l $LOC
az aks create -g $RG -n $AKS --location $LOC --node-count 1 \
  --node-vm-size Standard_DS2_v2_Promo --node-osdisk-size 32 --generate-ssh-keys --tier free
az aks approuting enable -g $RG -n $AKS      # managed NGINX ingress controller (AKS ships none by default)
az aks get-credentials -g $RG -n $AKS        # point kubectl at AKS
```
Control plane is free (`--tier free`); you pay only for the node VM. Region and VM size are heavily
constrained by the student subscription (see issues #6/#8/#9). The combo that actually works:
**`austriaeast`** + **`Standard_DS2_v2_Promo`** (2 vCPU / 7 GB, premium-storage capable). Delete
everything when done: `az group delete -n $RG --yes --no-wait`.

### The Azure overlay — `k8s-azure/`
| File | Purpose |
|---|---|
| `kustomization.yaml` | `resources: [../k8s]` (reuse the whole base) + patch the Ingress. Sibling of `k8s/`, not inside it, to avoid Kustomize's self-containment cycle. |
| `ingress-patch.yaml` | Swaps `ingressClassName: traefik` → **`webapprouting.kubernetes.azure.com`** (app-routing's NGINX) and drops the `expense.localhost` host so the rule is a **catch-all** reachable at the LB's public IP. |

Storage needs no change: the DB `volumeClaimTemplates` sets no `storageClassName`, so on AKS it uses
the default **`managed-csi`** (Azure Disk) automatically.

### First deploy + verify
```bash
kubectl apply -k k8s-azure
kubectl -n expense-tracker rollout status statefulset/db
kubectl -n expense-tracker rollout status deploy/backend deploy/frontend
IP=$(kubectl -n expense-tracker get ingress expense-tracker -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
curl -i "http://$IP/"                                   # SPA -> 200
curl -i -X POST "http://$IP/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"demo@demo.com","password":"demo1234"}'  # full chain -> 200
```

### Automated CD (rolling update from Docker Hub)
Kubernetes does **not** auto-pull a new image on a fixed tag. So the CD job pins the deployment to
this commit's **immutable `:<git-sha>`** tag — that tag change is what triggers a zero-downtime
`RollingUpdate`.

1. Store the AKS admin kubeconfig as a GitHub secret (cert-based → no service principal needed, which
   the university tenant may block):
   ```bash
   az aks get-credentials -g $RG -n $AKS --admin -f - | base64 | pbcopy
   ```
   GitHub → Settings → Secrets and variables → Actions → new secret **`KUBE_CONFIG`** (paste).
2. The `deploy` job in `.github/workflows/ci.yml` (`needs: [images]`, only on push to **develop**):
   installs kubectl + kustomize, decodes `KUBE_CONFIG`, runs
   `kustomize edit set image …:${{ github.sha }}`, `kubectl apply -k k8s-azure`, then
   `kubectl rollout status`. So: push to develop → `images` builds/pushes `:<sha>` → `deploy` rolls
   AKS onto it. Rollback: `kubectl -n expense-tracker rollout undo deploy/backend`.

The `deploy` job only rolls the app tiers; the Postgres StatefulSet is applied idempotently and keeps
its PVC across redeploys.

### Backend scaling caveat on Azure
Still `replicas: 1` because of the `@Scheduled` cron (see section 5). To scale it on AKS, add ShedLock
/ leader-election first, then bump replicas.
