CREATE SCHEMA IF NOT EXISTS commerce_dw;

CREATE TABLE IF NOT EXISTS commerce_dw.dim_customer (
  customer_sk BIGINT IDENTITY(1,1),
  customer_id VARCHAR(64) NOT NULL,
  segment VARCHAR(50),
  country VARCHAR(2),
  effective_from TIMESTAMP NOT NULL DEFAULT GETDATE(),
  effective_to TIMESTAMP,
  is_current BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (customer_sk)
) DISTSTYLE AUTO;

CREATE TABLE IF NOT EXISTS commerce_dw.dim_product (
  product_sk BIGINT IDENTITY(1,1),
  product_id VARCHAR(64) NOT NULL,
  product_name VARCHAR(256),
  category VARCHAR(128),
  effective_from TIMESTAMP NOT NULL DEFAULT GETDATE(),
  effective_to TIMESTAMP,
  is_current BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (product_sk)
) DISTSTYLE AUTO;

CREATE TABLE IF NOT EXISTS commerce_dw.fact_order (
  order_id VARCHAR(64) NOT NULL,
  customer_id VARCHAR(64) NOT NULL,
  order_ts TIMESTAMP NOT NULL,
  currency CHAR(3) NOT NULL,
  total_amount DECIMAL(18,2) NOT NULL,
  item_count INTEGER,
  PRIMARY KEY (order_id)
) DISTKEY(customer_id) SORTKEY(order_ts);

CREATE TABLE IF NOT EXISTS commerce_dw.fact_commerce_event (
  event_id VARCHAR(36) NOT NULL,
  event_type VARCHAR(80) NOT NULL,
  entity_type VARCHAR(40) NOT NULL,
  entity_id VARCHAR(128) NOT NULL,
  occurred_at TIMESTAMP NOT NULL,
  source VARCHAR(100),
  payload_json SUPER,
  PRIMARY KEY (event_id)
) DISTKEY(entity_id) SORTKEY(occurred_at);
