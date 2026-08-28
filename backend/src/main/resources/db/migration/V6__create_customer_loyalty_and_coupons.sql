ALTER TABLE customers ADD COLUMN points_balance INT NOT NULL DEFAULT 0;
ALTER TABLE customers ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE loyalty_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    amount_per_point DECIMAL(12,2) NOT NULL,
    minimum_points_to_redeem INT NOT NULL,
    maximum_redemption_percentage INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_loyalty_settings PRIMARY KEY (id),
    CONSTRAINT ck_loyalty_amount_per_point CHECK (amount_per_point > 0),
    CONSTRAINT ck_loyalty_minimum_points CHECK (minimum_points_to_redeem > 0),
    CONSTRAINT ck_loyalty_maximum_percentage CHECK (maximum_redemption_percentage BETWEEN 1 AND 100)
);

CREATE TABLE coupons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(40) NOT NULL,
    discount_type VARCHAR(25) NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    starts_at TIMESTAMP(6) NOT NULL,
    ends_at TIMESTAMP(6) NOT NULL,
    minimum_purchase DECIMAL(12,2) NOT NULL,
    total_usage_limit INT,
    per_customer_usage_limit INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_coupons PRIMARY KEY (id),
    CONSTRAINT uk_coupons_code UNIQUE (code),
    CONSTRAINT ck_coupons_type CHECK (discount_type IN ('PERCENTAGE','FIXED_AMOUNT')),
    CONSTRAINT ck_coupons_value CHECK (discount_value > 0),
    CONSTRAINT ck_coupons_dates CHECK (ends_at > starts_at),
    CONSTRAINT ck_coupons_total_limit CHECK (total_usage_limit IS NULL OR total_usage_limit > 0),
    CONSTRAINT ck_coupons_customer_limit CHECK (per_customer_usage_limit IS NULL OR per_customer_usage_limit > 0)
);

ALTER TABLE orders ADD COLUMN coupon_id BIGINT;
ALTER TABLE orders ADD COLUMN coupon_code_snapshot VARCHAR(40);
ALTER TABLE orders ADD COLUMN coupon_discount DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN points_redeemed INT NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN points_discount DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN points_earned INT NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN loyalty_awarded_at TIMESTAMP(6);
ALTER TABLE orders ADD COLUMN loyalty_reversed_at TIMESTAMP(6);
ALTER TABLE orders ADD CONSTRAINT fk_orders_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id);

CREATE TABLE coupon_redemptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    coupon_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    reversed_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_coupon_redemptions PRIMARY KEY (id),
    CONSTRAINT uk_coupon_redemptions_order UNIQUE (order_id),
    CONSTRAINT fk_coupon_redemptions_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    CONSTRAINT fk_coupon_redemptions_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_coupon_redemptions_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT ck_coupon_redemptions_discount CHECK (discount_amount >= 0)
);

CREATE TABLE loyalty_point_movements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    order_id BIGINT,
    created_by BIGINT,
    movement_type VARCHAR(30) NOT NULL,
    points_delta INT NOT NULL,
    balance_after INT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_loyalty_point_movements PRIMARY KEY (id),
    CONSTRAINT fk_loyalty_movement_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_loyalty_movement_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_loyalty_movement_user FOREIGN KEY (created_by) REFERENCES internal_users(id),
    CONSTRAINT uk_loyalty_order_movement UNIQUE (order_id, movement_type),
    CONSTRAINT ck_loyalty_movement_type CHECK (movement_type IN ('EARN','REDEEM','ADJUSTMENT','REVERSAL_EARN','REVERSAL_REDEEM')),
    CONSTRAINT ck_loyalty_movement_delta CHECK (points_delta <> 0),
    CONSTRAINT ck_loyalty_movement_balance CHECK (balance_after >= 0)
);

CREATE INDEX ix_customers_points ON customers(points_balance);
CREATE INDEX ix_coupons_active_dates ON coupons(active, starts_at, ends_at);
CREATE INDEX ix_coupon_redemptions_coupon ON coupon_redemptions(coupon_id, reversed_at);
CREATE INDEX ix_coupon_redemptions_customer ON coupon_redemptions(customer_id, coupon_id, reversed_at);
CREATE INDEX ix_loyalty_movements_customer ON loyalty_point_movements(customer_id, created_at);

INSERT INTO loyalty_settings (amount_per_point, minimum_points_to_redeem, maximum_redemption_percentage, active, created_at, updated_at)
VALUES (1000.00, 10, 30, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
