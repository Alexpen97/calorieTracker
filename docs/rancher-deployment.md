# NutriTrack — Rancher / Kubernetes Deployment Guide

## Overview

This guide covers deploying the full NutriTrack stack on a Kubernetes cluster managed by Rancher. All manifests live in `k8s/`.

### Architecture on K8s

```
Internet → Ingress (nginx) → frontend (:80)
                            → gateway  (:8080) → auth-service
                                                → user-profile-service
                                                → food-catalog-service
                                                → diary-service
                                                → nutrient-enrichment-service
                                                → nevo-service → libretranslate
PostgreSQL (5 logical DBs)   Redis (catalog cache)
```

---

## 1. Prerequisites

| Requirement | Minimum |
|---|---|
| Rancher | v2.8+ |
| Kubernetes | v1.28+ |
| kubectl + kustomize | latest |
| Container registry | Docker Hub, GHCR, or private |
| cert-manager (optional) | v1.14+ for auto TLS |
| Ingress controller | nginx-ingress (Rancher installs this via Apps) |

## 2. Install Rancher

### Option A — Docker (single-node, dev/test)

```bash
docker run -d --restart=unless-stopped \
  -p 80:80 -p 443:443 \
  --privileged \
  rancher/rancher:latest
```

Open `https://<server-ip>`, set admin password, done.

### Option B — Helm on existing K8s (production)

```bash
helm repo add rancher-stable https://releases.rancher.com/server-charts/stable
helm repo update

kubectl create namespace cattle-system

# Install cert-manager first
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.crds.yaml
helm repo add jetstack https://charts.jetstack.io
helm install cert-manager jetstack/cert-manager --namespace cert-manager --create-namespace

# Install Rancher
helm install rancher rancher-stable/rancher \
  --namespace cattle-system \
  --set hostname=rancher.yourdomain.com \
  --set replicas=3 \
  --set bootstrapPassword=admin
```

## 3. Create a Kubernetes Cluster in Rancher

1. Rancher UI → **Cluster Management** → **Create**
2. Choose provider (custom nodes, AWS, Azure, DigitalOcean, etc.)
3. Configure at least 3 nodes for production (2 CPU / 4 GB each minimum)
4. Wait for cluster to reach **Active** state
5. Download the kubeconfig from the Rancher UI

## 4. Build & Push Container Images

From the repo root, build and push each service image:

```bash
REGISTRY=your-registry.com/nutritrack  # e.g. ghcr.io/yourorg

# Infrastructure
docker build -t $REGISTRY/postgres:latest   ./infra/postgres
docker build -t $REGISTRY/redis:latest      ./infra/redis

# Backend services
for svc in auth-service user-profile-service food-catalog-service \
           diary-service nutrient-enrichment-service nevo-service \
           gateway libretranslate; do
  docker build -t $REGISTRY/$svc:latest ./services/$svc
done

# Frontend
docker build -t $REGISTRY/frontend:latest \
  --build-arg VITE_API_BASE_URL="" \
  --build-arg VITE_AUTH_MODE=dev \
  ./frontend

# Push all
docker push $REGISTRY/postgres:latest
docker push $REGISTRY/redis:latest
for svc in auth-service user-profile-service food-catalog-service \
           diary-service nutrient-enrichment-service nevo-service \
           gateway libretranslate frontend; do
  docker push $REGISTRY/$svc:latest
done
```

## 5. Update Image References

Edit `k8s/base/*.yaml` and replace `nutritrack/<service>:latest` with your actual registry prefix, or use kustomize `images` transformers in the overlay:

```yaml
# k8s/overlays/production/kustomization.yaml — add:
images:
  - name: nutritrack/postgres
    newName: your-registry.com/nutritrack/postgres
    newTag: v1.0.0
  # ... repeat for each service
```

## 6. Configure Secrets

Edit `k8s/base/secrets.yaml` with real values, **or** use Rancher's UI:

1. Rancher → your cluster → **Storage** → **Secrets**
2. Create `db-credentials` and `app-secrets` in the `nutritrack` namespace
3. Delete `k8s/base/secrets.yaml` from git (don't commit real secrets)

For production, consider **Sealed Secrets** or **External Secrets Operator**.

## 7. Deploy

```bash
# Point kubectl at your Rancher-managed cluster
export KUBECONFIG=~/.kube/rancher-cluster.yaml

# Staging
kubectl apply -k k8s/overlays/staging

# Production
kubectl apply -k k8s/overlays/production

# Verify
kubectl -n nutritrack get pods
kubectl -n nutritrack get ingress
```

## 8. Configure Ingress & DNS

1. Get the Ingress external IP: `kubectl -n nutritrack get ingress`
2. Create a DNS A record: `nutritrack.yourdomain.com` → external IP
3. If using cert-manager, TLS is auto-provisioned via the `letsencrypt-prod` ClusterIssuer

### Install ClusterIssuer for Let's Encrypt

```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: you@yourdomain.com
    privateKeySecretRef:
      name: letsencrypt-prod-key
    solvers:
      - http01:
          ingress:
            class: nginx
```

## 9. Monitoring (Rancher built-in)

Rancher includes Prometheus + Grafana via **Monitoring** app:

1. Rancher → cluster → **Apps** → **Charts** → install **Monitoring**
2. Grafana is accessible from the Rancher UI sidebar
3. Spring Boot Actuator endpoints (`/actuator/prometheus`) can be scraped — add ServiceMonitor resources if needed

## 10. Scaling

Scale services via Rancher UI (Workloads → scale slider) or:

```bash
kubectl -n nutritrack scale deployment gateway --replicas=3
kubectl -n nutritrack scale deployment frontend --replicas=3
```

For auto-scaling, add HorizontalPodAutoscaler resources.

## 11. CI/CD Integration

Add a GitHub Actions workflow to build images and deploy on push:

```yaml
# .github/workflows/deploy-k8s.yml (example)
name: Deploy to Rancher K8s
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: Build & push images
        run: |
          REGISTRY=ghcr.io/${{ github.repository_owner }}/nutritrack
          # build loop as shown in section 4
      - name: Deploy
        run: |
          echo "${{ secrets.KUBECONFIG }}" > /tmp/kubeconfig
          KUBECONFIG=/tmp/kubeconfig kubectl apply -k k8s/overlays/production
          KUBECONFIG=/tmp/kubeconfig kubectl -n nutritrack rollout restart deployment
```

## Comparison: Railway vs Rancher/K8s

| Aspect | Railway (current) | Rancher + K8s |
|---|---|---|
| Setup effort | Minimal | Moderate (cluster + Rancher) |
| Cost | Per-usage billing | You manage infra costs |
| Scaling | Manual per-service | HPA, cluster autoscaler |
| Networking | Railway private net | K8s Services (ClusterIP) |
| Secrets | Railway env vars | K8s Secrets / Sealed Secrets |
| Monitoring | Railway metrics | Prometheus + Grafana |
| Control | Limited | Full |
| Multi-env | Separate Railway projects | Kustomize overlays |
