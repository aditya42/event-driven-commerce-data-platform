# Event-Driven Commerce Data Platform

A portfolio-grade data engineering project that models a commerce event platform using **Java 21, Spring Boot, Apache Kafka, Kafka Connect, PostgreSQL, Python, PySpark, AWS S3/Glue/Athena/Redshift concepts, Docker, Kubernetes, Helm and GitHub Actions**.

The project is deliberately built as a small but credible platform rather than eight toy microservices. The order service owns transactional order state and publishes domain events. The ingestion service consumes commerce events idempotently, persists an operational event ledger, and lands immutable raw events in an S3 data lake. PySpark jobs curate the lake and reconcile transactional orders against ingested event data.

## Architecture

```text
Client / Demo Producer
        |
        v
+----------------------+       +------------------+
| Order Service        | ----> | Apache Kafka     |
| Java 21/Spring Boot  |       | domain topics    |
| PostgreSQL orders    |       +--------+---------+
+----------------------+                |
                                        v
                              +----------------------+
                              | Stream Ingestion     |
                              | Spring Boot Consumer |
                              +-----+-----------+----+
                                    |           |
                              Postgres           S3 raw
                           event ledger          data lake
                                    |           |
                                    +-----+-----+
                                          v
                                      PySpark
                                          |
                                      curated/
                                     Parquet data
                                       /      \
                                      v        v
                                   Athena   Redshift

Order DB <------------------ reconciliation -----------------> Event ledger/lake
```

Commerce domains represented by the event model: `customer`, `product`, `inventory`, `cart`, `order`, `payment`, `shipment`, and `refund`.

## What this demonstrates

- Event-driven service design, transactional outbox delivery, and Kafka partitioning by business entity.
- Transactional operational data in PostgreSQL plus an immutable event ledger.
- Idempotent stream consumption using `event_id` as the natural deduplication key.
- S3-style bronze/raw and curated/Parquet lake zones.
- PySpark transformation, schema normalization, data-quality checks, and partitioned output.
- Source-to-lake reconciliation for missing or duplicated order events.
- Optional Kafka Connect JDBC sink configuration.
- Local AWS emulation with LocalStack; AWS Glue/Athena/Redshift deployment assets are included.
- Docker Compose for local execution and Helm/Kubernetes manifests for deployment.
- CI build, unit tests, Python tests, and manifest linting.

## Quick start

Prerequisite: Docker Desktop / Docker Engine with Compose v2.

```bash
cp .env.example .env
make up
make smoke
make demo
make curate
make reconcile
```

Service endpoints:

- Order service: `http://localhost:8080`
- Ingestion health: `http://localhost:8081/actuator/health`
- LocalStack S3: `http://localhost:4566`
- Kafka from host: `localhost:29092`
- PostgreSQL: `localhost:5432`

## Example order

```bash
curl -sS -X POST http://localhost:8080/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "cust-1001",
    "currency": "USD",
    "items": [
      {"productId": "sku-video-001", "quantity": 1, "unitPrice": 14.99},
      {"productId": "sku-hdmi-004", "quantity": 2, "unitPrice": 9.50}
    ]
  }' | python3 -m json.tool
```

The service writes the order **and an outbox row in the same PostgreSQL transaction**. A scheduled outbox relay publishes `ORDER_CREATED` to `commerce.order.v1`; the downstream consumer is idempotent so relay retries are safe. The ingestion service stores that event in `ingested_events` and writes the same event to:

```text
s3://commerce-data-lake/raw/entity_type=order/event_date=YYYY-MM-DD/<event_id>.json
```

## Demo all commerce domains

`make demo` creates an order and then emits realistic events for payment authorization, inventory reservation, shipment, refund, customer, product, and cart activity through the generic domain-event endpoint.

## Curated lake

```bash
make curate
```

The PySpark job:

1. Downloads raw JSON objects from S3.
2. Applies a canonical event schema.
3. Deduplicates on `event_id`.
4. Separates malformed records into a quarantine prefix.
5. Writes partitioned Parquet to `s3://commerce-data-lake/curated/commerce_events/`.
6. Publishes a small data-quality report to `quality/`.

## Reconciliation

```bash
make reconcile
```

This compares order IDs in the source `orders` table with successfully ingested `ORDER_CREATED` events. A JSON report is written to `reports/reconciliation.json` and to the lake under `quality/reconciliation/`. A non-zero exit code is returned when the source and ingestion layers diverge.

## Optional Kafka Connect

The core platform does not depend on Kafka Connect, so the default demo remains reliable and quick. To run the optional JDBC sink worker:

```bash
make connect
./scripts/register-connectors.sh
```

The connector mirrors selected domain events to `reports/connect/commerce-events.log`, giving you a visible Kafka Connect demo without making the core pipeline depend on Connect. In a production variant, replace this with a governed sink such as S3, JDBC, or a CDC connector.

## AWS deployment path

LocalStack only replaces S3 for local development. In AWS:

- Replace `APP_S3_ENDPOINT` with an empty value so the AWS SDK uses native S3.
- Run `data-platform/glue/commerce_curated_job.py` as an AWS Glue Spark job.
- Execute `data-platform/athena/create_tables.sql` in Athena after curating data.
- Use `data-platform/redshift/create_star_schema.sql` as the warehouse starting schema.
- Store DB credentials in Secrets Manager and attach IAM roles rather than static keys.
- Use MSK for Kafka if keeping Kafka as the streaming backbone.

See `docs/architecture.md`, `docs/data-model.md`, and `docs/demo-script.md` for interview-ready explanations.


## Flyway recovery for existing local databases

If an older local database was baselined by Flyway before the order-service V1
migration ran, the `orders` and `order_items` tables may be absent although
`outbox_events` exists. Order creation then fails with `relation "orders" does not exist`.

This repository includes the **V3__repair_baselined_order_tables.sql** migration and
a **V2__repair_baselined_ingested_events.sql** migration for the ingestion service.
It creates missing order tables and indexes with `IF NOT EXISTS`. It is
forward-only and does not delete records or rewrite Flyway history. It also
works on fresh databases where V1 created the tables already.

Upgrade an existing environment (preserves Docker volumes):

```bash
docker compose up -d --build order-service
docker compose exec -T postgres psql -U commerce -d commerce -c "SELECT version, description, success FROM flyway_order_service_history ORDER BY installed_rank;"
docker compose exec -T postgres psql -U commerce -d commerce -c "SELECT to_regclass('public.orders') AS orders, to_regclass('public.order_items') AS order_items;"
make smoke
make demo
```

The resulting Flyway history should include successful version 3, and both
order tables should exist. **Do not use `docker compose down -v`** for an
upgrade, because that deletes the database volume. Both local services set `baseline-on-migrate: true` because they share the
`public` schema and may start in either order. A production design should
use separate ownership schemas (and independent migration histories) instead.
Do not carry local automatic baselining into production without verification.
