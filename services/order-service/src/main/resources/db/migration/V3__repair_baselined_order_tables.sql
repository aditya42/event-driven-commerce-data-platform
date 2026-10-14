-- Repair databases where Flyway baselined version 1 without executing V1__orders.sql.
-- Safe for fresh databases where V1 created these objects: no data is dropped.
CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    currency CHAR(3) NOT NULL,
    total_amount NUMERIC(14,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- An earlier local/manual recovery may have created orders.order_id as UUID.
-- The Java repository and original V1 migration require VARCHAR(64).
-- Convert existing UUID values to canonical text before adding the FK.
-- PostgreSQL transactional DDL makes this retryable after a failed V3 run.
DO $$
DECLARE
    id_type text;
BEGIN
    SELECT format_type(a.atttypid, a.atttypmod)
      INTO id_type
      FROM pg_attribute a
      JOIN pg_class c ON c.oid = a.attrelid
      JOIN pg_namespace n ON n.oid = c.relnamespace
     WHERE n.nspname = current_schema()
       AND c.relname = 'orders'
       AND a.attname = 'order_id'
       AND a.attnum > 0
       AND NOT a.attisdropped;

    IF id_type = 'uuid' THEN
        ALTER TABLE orders
            ALTER COLUMN order_id TYPE VARCHAR(64) USING order_id::text;
    ELSIF id_type IS NULL OR id_type NOT IN ('character varying(64)', 'character varying(128)', 'character varying') THEN
        RAISE EXCEPTION 'Unexpected orders.order_id type: %; inspect before migrating', id_type;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS order_items (
    order_id VARCHAR(64) NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    line_no INTEGER NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(14,2) NOT NULL CHECK (unit_price >= 0),
    PRIMARY KEY (order_id, line_no)
);

CREATE INDEX IF NOT EXISTS idx_orders_customer_created ON orders(customer_id, created_at DESC);
