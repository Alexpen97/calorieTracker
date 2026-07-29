# Rancher / Kubernetes Deployment

## What was done

- Created full Kubernetes manifests in `k8s/base/` for all 10 NutriTrack services + PostgreSQL + Redis
- Kustomize structure with `base`, `overlays/production`, `overlays/staging`
- Ingress with TLS (cert-manager) routing `/api` → gateway, `/` → frontend
- All secrets extracted to K8s Secret resources (template)
- Resource requests/limits, readiness/liveness probes on every pod
- Comprehensive deployment guide at `docs/rancher-deployment.md`

## File structure

```
k8s/
├── base/
│   ├── kustomization.yaml
│   ├── namespace.yaml
│   ├── secrets.yaml          # template — don't commit real values
│   ├── postgres.yaml
│   ├── redis.yaml
│   ├── auth-service.yaml
│   ├── user-profile-service.yaml
│   ├── food-catalog-service.yaml
│   ├── diary-service.yaml
│   ├── nutrient-enrichment-service.yaml
│   ├── nevo-service.yaml
│   ├── libretranslate.yaml
│   ├── gateway.yaml
│   ├── frontend.yaml
│   └── ingress.yaml
└── overlays/
    ├── production/kustomization.yaml   # 2 replicas for key services
    └── staging/kustomization.yaml
```

## Next steps

- [ ] Set up container registry and push images
- [ ] Install Rancher on target infrastructure
- [ ] Create K8s cluster via Rancher
- [ ] Replace placeholder secrets with real values
- [ ] Replace `nutritrack.example.com` with real domain
- [ ] Consider PostgreSQL operator (CloudNativePG) for production instead of single-pod Postgres
- [ ] Add HorizontalPodAutoscaler for traffic-heavy services
- [ ] Set up CI/CD pipeline for automated image builds and deploys

## 2026-07-29 local Rancher Desktop follow-up

- Added `k8s/overlays/local/kustomization.yaml` for local Traefik-based deployment.
- Fixed `services/libretranslate/docker-entrypoint.sh` to locate the real upstream executable at `/app/venv/bin/libretranslate` when it is not on `PATH`.
- Added `services/libretranslate/smoke-test.sh` to build the image and verify `/languages` responds on a temporary local container.
- Dev Login empty **403**: gateway `CORS_ALLOWED_ORIGINS` was `https://nutritrack.example.com`. Browser POSTs from `http://localhost:8088` are rejected before auth-service. Local overlay now allows `http://localhost:8088` (+ vite/capacitor localhost origins).
