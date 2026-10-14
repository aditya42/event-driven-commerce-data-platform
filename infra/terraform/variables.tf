variable "aws_region" { type = string default = "us-east-1" }
variable "data_lake_bucket" { type = string description = "Globally unique S3 bucket name" }
variable "glue_database" { type = string default = "commerce_analytics" }
