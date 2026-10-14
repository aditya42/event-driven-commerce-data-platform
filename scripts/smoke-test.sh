#!/usr/bin/env bash
set -euo pipefail

for url in http://localhost:8080/actuator/health http://localhost:8081/actuator/health; do
  echo "Checking $url"
  curl -fsS "$url" | grep -q 'UP'
done

curl -fsS http://localhost:8080/api/v1/orders >/dev/null
docker compose exec -T postgres psql -U "${POSTGRES_USER:-commerce}" -d "${POSTGRES_DB:-commerce}" -c 'select 1' >/dev/null
docker compose exec -T localstack awslocal s3api head-bucket --bucket "${S3_BUCKET:-commerce-data-lake}" >/dev/null

echo "Smoke test passed"
