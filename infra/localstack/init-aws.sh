#!/usr/bin/env bash
set -euo pipefail
awslocal s3api create-bucket --bucket "${S3_BUCKET:-commerce-data-lake}" >/dev/null 2>&1 || true
awslocal s3api put-bucket-versioning --bucket "${S3_BUCKET:-commerce-data-lake}" --versioning-configuration Status=Enabled
echo "Local S3 bucket ready: ${S3_BUCKET:-commerce-data-lake}"
