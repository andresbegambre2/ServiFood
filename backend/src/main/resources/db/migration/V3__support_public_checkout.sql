ALTER TABLE business_settings ADD COLUMN time_zone VARCHAR(60) NOT NULL DEFAULT 'America/Bogota';
ALTER TABLE business_settings ADD COLUMN transfer_provider VARCHAR(120);
ALTER TABLE business_settings ADD COLUMN transfer_account_holder VARCHAR(150);
ALTER TABLE business_settings ADD COLUMN transfer_account_reference VARCHAR(120);
ALTER TABLE business_settings ADD COLUMN payment_qr_path VARCHAR(500);

ALTER TABLE orders ADD COLUMN client_request_id VARCHAR(36);
ALTER TABLE orders ADD COLUMN tracking_token_hash VARCHAR(64);
ALTER TABLE orders ADD COLUMN customer_email_snapshot VARCHAR(190);
ALTER TABLE orders ADD COLUMN estimated_minutes INT;
ALTER TABLE orders ADD CONSTRAINT uk_orders_client_request_id UNIQUE (client_request_id);
CREATE INDEX ix_orders_tracking ON orders(public_number, tracking_token_hash);

ALTER TABLE payments ADD COLUMN cash_tendered DECIMAL(12,2);
