# AWS Terraform starter

This module provisions the low-cost shared data-lake layer: versioned/private S3, a Glue Data Catalog database, and an Athena workgroup. Kafka/MSK and Redshift are intentionally not created by default because they can incur material ongoing cost. Add them when you want a full cloud deployment.

```bash
terraform init
terraform apply -var='data_lake_bucket=<globally-unique-bucket-name>'
```
