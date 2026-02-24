# Kubernetes deployment

Frontend, backend, and database are separate deployments. Use one Ingress so the browser uses a single origin: route `/` and frontend paths to the frontend service, and `/api`, `/login`, `/logout`, `/oauth2` to the backend service.

## Where the DB is deployed

| Environment | Database | Where |
|-------------|----------|--------|
| **Local** (`docker compose up`) | PostgreSQL | Postgres in Docker (volume `pgdata`). Backend env: `SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/cyph`. |
| **Kubernetes** | PostgreSQL | Deployed in the cluster via `deploy/kubernetes/postgres-deployment.yaml` (StatefulSet + Service named `postgres`). Backend uses env `DB_HOST=postgres`, `DB_PORT=5432`, `DB_NAME=cyph`, `DB_USER=cyph`, `DB_PASSWORD` from Secret. |
| **Docker (production)** | PostgreSQL | Use `deploy/docker/` images and a compose file with Postgres and config mount. |
| **VM / other** | You run Postgres (or MySQL) yourself and set `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` (or `SPRING_DATASOURCE_URL`) for the backend. |

## Deploy steps

1. Build and push images: `cyph-backend:latest`, `cyph-frontend:latest`.

2. Create ConfigMap from `configmap.example.yaml`. Set `SITE_URL` to your public URL. The example already sets `DB_HOST=postgres`, `DB_PORT=5432`, `DB_NAME=cyph` for the in-cluster Postgres.

3. Create Secret `cyph-secrets` with at least `DB_PASSWORD` (and optionally `OIDC_CLIENT_SECRET`, `SMTP_PASSWORD`, etc.).

4. Apply in order:
   - ConfigMap and Secret
   - `postgres-deployment.yaml` (so the DB is running before the backend)
   - `backend-deployment.yaml` (backend envFrom: cyph-config + cyph-secrets)
   - `frontend-deployment.yaml`
   - Ingress (e.g. `ingress.example.yaml`)

5. Expose via Ingress or LoadBalancer; route `/api`, `/login`, `/logout`, `/oauth2` to the backend and everything else to the frontend.
