ALTER TABLE orders ADD COLUMN on_the_way_at TIMESTAMP(6);
ALTER TABLE orders ADD COLUMN cancellation_reason VARCHAR(500);
