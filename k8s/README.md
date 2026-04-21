# mk1 Kubernetes (Learning-Friendly Standard)

This folder provides a practical baseline for running `mk1` on Kubernetes without overengineering.

## What is included

- `Namespace`
- `ConfigMap` + `Secret`
- `Postgres` (StatefulSet + PVC + Service)
- `Redis` (Deployment + PVC + Service)
- `App` (Deployment + Service)
- `Ingress`

## Apply

1) Create local secret file (not committed):

```bash
cp k8s/base/secret.example.yaml k8s/base/secret.yaml
# then edit values in secret.yaml
```

2) Apply:

```bash
kubectl apply -k k8s/base
kubectl -n pos-mk1 get all
```

## AWS overlay

Use this when DB/Redis are external (RDS/ElastiCache) and only app workloads run on EKS.

1) Prepare overlay secret:

```bash
cp k8s/overlays/aws/secret.aws.example.yaml k8s/overlays/aws/secret.aws.yaml
# edit RDS/Redis/JWT values
```

2) In `k8s/overlays/aws/kustomization.yaml`, replace ECR image URIs.

3) Apply:

```bash
kubectl apply -k k8s/overlays/aws
kubectl -n pos-mk1 get all
```

## Notes for your AWS plan

- `mk1`, `mk2`, `mk3` can run on the `t3.large` cluster.
- Elasticsearch can run on a separate `t3.small`.
- For that split, keep `POS_SEARCH_ELASTICSEARCH_ENABLED=true` and set:
  - `POS_ELASTICSEARCH_URIS=http://<elasticsearch-host>:9200`
- If ES is temporarily off, set:
  - `POS_SEARCH_ELASTICSEARCH_ENABLED=false`

## Before real AWS use

- Move secret values to AWS Secrets Manager / External Secrets.
- Replace image names with your ECR images.
- Add TLS and DNS host in `ingress.yaml`.
