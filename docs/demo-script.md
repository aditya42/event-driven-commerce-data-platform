# Five-Minute Interview Demo

1. Start with the architecture in the README and explain the source/stream/lake split.
2. Run `make up` and `make smoke`.
3. Run `make demo` to generate order/payment/inventory/shipment/refund traffic.
4. Show rows in PostgreSQL:
   `docker compose exec postgres psql -U commerce -d commerce -c 'select event_type, count(*) from ingested_events group by 1 order by 1;'`
5. Show S3 raw objects:
   `docker compose exec localstack awslocal s3 ls s3://commerce-data-lake/raw/ --recursive | head`
6. Run `make curate`, then show curated Parquet objects.
7. Run `make reconcile` and open `reports/reconciliation.json`.
8. Close by explaining how the same code maps to MSK + S3 + Glue + Athena/Redshift in AWS and what you would harden for production.
