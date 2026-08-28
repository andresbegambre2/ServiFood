CREATE INDEX ix_orders_created_at ON orders(created_at);
CREATE INDEX ix_coupon_redemptions_created ON coupon_redemptions(created_at, reversed_at);
CREATE INDEX ix_loyalty_movements_type_created ON loyalty_point_movements(movement_type, created_at);
CREATE INDEX ix_payments_method_created ON payments(method, created_at);
