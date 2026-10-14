# Data Model

## Transactional table: `orders`

`orders` is the source-of-truth order header. `order_items` stores line items. The example intentionally keeps payment, shipment, refund and inventory data as events rather than building a complete commerce OLTP schema.

## Event ledger: `ingested_events`

The ingestion ledger is optimized for traceability and reconciliation. Important columns include:

- `event_id` UUID primary key
- `event_type`
- `entity_type`
- `entity_id`
- `occurred_at`
- `source`
- `schema_version`
- `payload` JSONB
- `s3_key`
- `ingested_at`

## Commerce domains

| Domain | Representative events |
|---|---|
| customer | CUSTOMER_CREATED, CUSTOMER_UPDATED |
| product | PRODUCT_CREATED, PRODUCT_PRICE_CHANGED |
| inventory | INVENTORY_RESERVED, INVENTORY_RELEASED |
| cart | CART_CREATED, CART_ITEM_ADDED, CART_CHECKED_OUT |
| order | ORDER_CREATED, ORDER_CANCELLED |
| payment | PAYMENT_AUTHORIZED, PAYMENT_DECLINED, PAYMENT_CAPTURED |
| shipment | SHIPMENT_CREATED, SHIPMENT_DISPATCHED, SHIPMENT_DELIVERED |
| refund | REFUND_REQUESTED, REFUND_COMPLETED |

## Warehouse starter model

The Redshift SQL contains conformed customer/product dimensions plus order and event facts. It is intentionally a starter model: in a production warehouse, SCD strategy, late-arriving dimensions, currency normalization and delete semantics would be requirements-driven.
