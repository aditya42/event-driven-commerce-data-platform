#!/usr/bin/env bash
set -euo pipefail
BASE=http://localhost:8080

ORDER_JSON=$(curl -fsS -X POST "$BASE/api/v1/orders" -H 'Content-Type: application/json' -d '{
  "customerId": "cust-1001",
  "currency": "USD",
  "items": [
    {"productId": "sku-video-001", "quantity": 1, "unitPrice": 14.99},
    {"productId": "sku-hdmi-004", "quantity": 2, "unitPrice": 9.50}
  ]
}')

ORDER_ID=$(python3 -c 'import json,sys; print(json.load(sys.stdin)["orderId"])' <<<"$ORDER_JSON")
echo "Created order: $ORDER_ID"

emit() {
  curl -fsS -X POST "$BASE/api/v1/events" -H 'Content-Type: application/json' -d "$1" >/dev/null
}

emit '{"eventType":"CUSTOMER_UPDATED","entityType":"customer","entityId":"cust-1001","source":"customer-service","payload":{"segment":"prime","country":"US"}}'
emit '{"eventType":"PRODUCT_PRICE_CHANGED","entityType":"product","entityId":"sku-video-001","source":"catalog-service","payload":{"oldPrice":16.99,"newPrice":14.99,"currency":"USD"}}'
emit "{\"eventType\":\"INVENTORY_RESERVED\",\"entityType\":\"inventory\",\"entityId\":\"sku-video-001\",\"source\":\"inventory-service\",\"payload\":{\"orderId\":\"$ORDER_ID\",\"quantity\":1}}"
emit '{"eventType":"CART_CHECKED_OUT","entityType":"cart","entityId":"cart-9001","source":"cart-service","payload":{"customerId":"cust-1001"}}'
emit "{\"eventType\":\"PAYMENT_AUTHORIZED\",\"entityType\":\"payment\",\"entityId\":\"pay-7001\",\"source\":\"payment-service\",\"payload\":{\"orderId\":\"$ORDER_ID\",\"amount\":33.99,\"currency\":\"USD\"}}"
emit "{\"eventType\":\"SHIPMENT_DISPATCHED\",\"entityType\":\"shipment\",\"entityId\":\"ship-5001\",\"source\":\"shipment-service\",\"payload\":{\"orderId\":\"$ORDER_ID\",\"carrier\":\"UPS\"}}"
emit "{\"eventType\":\"REFUND_REQUESTED\",\"entityType\":\"refund\",\"entityId\":\"refund-3001\",\"source\":\"refund-service\",\"payload\":{\"orderId\":\"$ORDER_ID\",\"amount\":9.50,\"reason\":\"customer_return\"}}"

sleep 3
echo
echo "Event counts:"
docker compose exec -T postgres psql -U "${POSTGRES_USER:-commerce}" -d "${POSTGRES_DB:-commerce}" -c 'select event_type, count(*) from ingested_events group by 1 order by 1;'
echo "Raw lake objects:"
docker compose exec -T localstack awslocal s3 ls "s3://${S3_BUCKET:-commerce-data-lake}/raw/" --recursive | head -20
