# Production Hardening Roadmap

This repository is runnable and demonstrates the complete data path. For a production implementation, prioritize these changes:

1. **CDC-based outbox relay**: this repo already writes order + outbox atomically; replace the polling relay with Debezium/Kafka Connect when you need lower-latency, high-throughput CDC.
2. **Schema Registry**: enforce Avro/Protobuf compatibility rather than JSON-only contracts.
3. **Managed streaming**: run Kafka on Amazon MSK, use IAM/SASL authentication, TLS, quotas, topic policies and multi-AZ replication.
4. **S3 governance**: SSE-KMS, bucket policies, object lock where required, lifecycle rules, Lake Formation permissions and data classification.
5. **DLQ strategy**: route poison messages to versioned dead-letter topics and expose replay tooling.
6. **Observability**: OpenTelemetry traces, consumer lag metrics, event age, S3 write latency, DQ metrics and reconciliation alarms.
7. **Warehouse ingestion**: automate COPY/MERGE into Redshift or use an Iceberg lakehouse strategy depending on query patterns.
8. **Secrets and identity**: IAM roles for service accounts, Secrets Manager, no long-lived AWS credentials.
9. **Contract and load testing**: compatibility tests, Testcontainers integration tests, partition-key skew tests and backpressure/load scenarios.
10. **IaC**: provision AWS components using Terraform/CDK and enforce policy-as-code in CI.
