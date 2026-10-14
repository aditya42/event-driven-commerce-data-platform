#!/usr/bin/env bash
set -euo pipefail
mkdir -p reports/connect
curl -fsS -X PUT http://localhost:8083/connectors/commerce-file-audit-sink/config \
  -H 'Content-Type: application/json' \
  --data-binary @<(python3 - <<'PY'
import json
p='infra/kafka-connect/connectors/file-audit-sink.json'
d=json.load(open(p))
print(json.dumps(d['config']))
PY
) | python3 -m json.tool
