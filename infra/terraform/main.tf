terraform {
  required_version = ">= 1.6"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.0" }
  }
}

provider "aws" { region = var.aws_region }

resource "aws_s3_bucket" "lake" { bucket = var.data_lake_bucket }

resource "aws_s3_bucket_versioning" "lake" {
  bucket = aws_s3_bucket.lake.id
  versioning_configuration { status = "Enabled" }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "lake" {
  bucket = aws_s3_bucket.lake.id
  rule { apply_server_side_encryption_by_default { sse_algorithm = "AES256" } }
}

resource "aws_s3_bucket_public_access_block" "lake" {
  bucket = aws_s3_bucket.lake.id
  block_public_acls = true
  block_public_policy = true
  ignore_public_acls = true
  restrict_public_buckets = true
}

resource "aws_glue_catalog_database" "commerce" { name = var.glue_database }

resource "aws_athena_workgroup" "commerce" {
  name = "commerce-analytics"
  configuration {
    enforce_workgroup_configuration = true
    result_configuration { output_location = "s3://${aws_s3_bucket.lake.bucket}/athena-results/" }
  }
}

output "data_lake_bucket" { value = aws_s3_bucket.lake.bucket }
output "glue_database" { value = aws_glue_catalog_database.commerce.name }
