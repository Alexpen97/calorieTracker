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
