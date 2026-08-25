CREATE TABLE internal_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(190) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_internal_users PRIMARY KEY (id),
    CONSTRAINT uk_internal_users_email UNIQUE (email),
    CONSTRAINT ck_internal_users_role CHECK (role IN ('ADMIN', 'CASHIER', 'KITCHEN'))
);

CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT uk_categories_slug UNIQUE (slug),
    CONSTRAINT ck_categories_order CHECK (display_order >= 0)
);

CREATE TABLE extras (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(12,2) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_extras PRIMARY KEY (id),
    CONSTRAINT uk_extras_name UNIQUE (name),
    CONSTRAINT ck_extras_price CHECK (price >= 0)
);

CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(170) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    image_path VARCHAR(500),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_slug UNIQUE (slug),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT ck_products_price CHECK (price >= 0)
);
CREATE INDEX ix_products_category_available ON products(category_id, available);
CREATE INDEX ix_products_featured ON products(featured, available);

CREATE TABLE product_extras (
    product_id BIGINT NOT NULL,
    extra_id BIGINT NOT NULL,
    CONSTRAINT pk_product_extras PRIMARY KEY (product_id, extra_id),
    CONSTRAINT fk_product_extras_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_extras_extra FOREIGN KEY (extra_id) REFERENCES extras(id)
);

CREATE TABLE customers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(190),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (id)
);
CREATE INDEX ix_customers_phone ON customers(phone);
CREATE INDEX ix_customers_email ON customers(email);

CREATE TABLE customer_addresses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    label VARCHAR(50),
    address VARCHAR(250) NOT NULL,
    neighborhood VARCHAR(120) NOT NULL,
    reference VARCHAR(500),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_customer_addresses PRIMARY KEY (id),
    CONSTRAINT fk_customer_addresses_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);
CREATE INDEX ix_customer_addresses_customer ON customer_addresses(customer_id);

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_number VARCHAR(30) NOT NULL,
    customer_id BIGINT,
    customer_name_snapshot VARCHAR(120) NOT NULL,
    customer_phone_snapshot VARCHAR(30) NOT NULL,
    delivery_type VARCHAR(20) NOT NULL,
    delivery_address_snapshot VARCHAR(500),
    subtotal DECIMAL(12,2) NOT NULL,
    delivery_fee DECIMAL(12,2) NOT NULL,
    discount DECIMAL(12,2) NOT NULL,
    total DECIMAL(12,2) NOT NULL,
    notes VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    confirmed_at TIMESTAMP(6), prepared_at TIMESTAMP(6), ready_at TIMESTAMP(6),
    delivered_at TIMESTAMP(6), cancelled_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uk_orders_public_number UNIQUE (public_number),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT ck_orders_delivery_type CHECK (delivery_type IN ('DELIVERY', 'PICKUP')),
    CONSTRAINT ck_orders_status CHECK (status IN ('NEW','CONFIRMED','PREPARING','READY','ON_THE_WAY','DELIVERED','CANCELLED')),
    CONSTRAINT ck_orders_amounts CHECK (subtotal >= 0 AND delivery_fee >= 0 AND discount >= 0 AND total >= 0)
);
CREATE INDEX ix_orders_status_created ON orders(status, created_at);
CREATE INDEX ix_orders_customer ON orders(customer_id);
CREATE INDEX ix_orders_delivery_type ON orders(delivery_type);

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    product_name_snapshot VARCHAR(150) NOT NULL,
    unit_price_snapshot DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    notes VARCHAR(500),
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_amounts CHECK (unit_price_snapshot >= 0 AND subtotal >= 0)
);
CREATE INDEX ix_order_items_order ON order_items(order_id);
CREATE INDEX ix_order_items_product ON order_items(product_id);

CREATE TABLE order_item_extras (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_item_id BIGINT NOT NULL,
    extra_id BIGINT,
    extra_name_snapshot VARCHAR(120) NOT NULL,
    unit_price_snapshot DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    CONSTRAINT pk_order_item_extras PRIMARY KEY (id),
    CONSTRAINT fk_order_item_extras_item FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_extras_extra FOREIGN KEY (extra_id) REFERENCES extras(id),
    CONSTRAINT ck_order_item_extras_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_item_extras_amounts CHECK (unit_price_snapshot >= 0 AND subtotal >= 0)
);
CREATE INDEX ix_order_item_extras_item ON order_item_extras(order_item_id);

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    method VARCHAR(25) NOT NULL,
    status VARCHAR(25) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    receipt_path VARCHAR(500),
    rejection_reason VARCHAR(500),
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_payments_reviewer FOREIGN KEY (reviewed_by) REFERENCES internal_users(id),
    CONSTRAINT ck_payments_method CHECK (method IN ('CASH','TRANSFER','PAY_ON_PICKUP')),
    CONSTRAINT ck_payments_status CHECK (status IN ('PENDING','UNDER_REVIEW','APPROVED','REJECTED')),
    CONSTRAINT ck_payments_amount CHECK (amount >= 0)
);
CREATE INDEX ix_payments_order ON payments(order_id);
CREATE INDEX ix_payments_status ON payments(status, created_at);

CREATE TABLE promotions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    discount_type VARCHAR(25) NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    starts_at TIMESTAMP(6) NOT NULL,
    ends_at TIMESTAMP(6) NOT NULL,
    minimum_purchase DECIMAL(12,2) NOT NULL,
    usage_limit INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_promotions PRIMARY KEY (id),
    CONSTRAINT ck_promotions_type CHECK (discount_type IN ('PERCENTAGE','FIXED_AMOUNT')),
    CONSTRAINT ck_promotions_value CHECK (discount_value >= 0),
    CONSTRAINT ck_promotions_dates CHECK (ends_at > starts_at),
    CONSTRAINT ck_promotions_usage CHECK (usage_limit IS NULL OR usage_limit > 0)
);
CREATE INDEX ix_promotions_active_dates ON promotions(active, starts_at, ends_at);

CREATE TABLE business_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trade_name VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    logo_path VARCHAR(500),
    phone VARCHAR(30) NOT NULL,
    whatsapp VARCHAR(30) NOT NULL,
    address VARCHAR(300) NOT NULL,
    instagram VARCHAR(200),
    facebook VARCHAR(200),
    base_delivery_fee DECIMAL(12,2) NOT NULL,
    estimated_preparation_minutes INT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_business_settings PRIMARY KEY (id),
    CONSTRAINT ck_business_delivery_fee CHECK (base_delivery_fee >= 0),
    CONSTRAINT ck_business_preparation CHECK (estimated_preparation_minutes > 0)
);

CREATE TABLE business_hours (
    id BIGINT NOT NULL AUTO_INCREMENT,
    day_of_week VARCHAR(12) NOT NULL,
    slot_number INT NOT NULL DEFAULT 1,
    opens_at TIME,
    closes_at TIME,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_business_hours PRIMARY KEY (id),
    CONSTRAINT uk_business_hours_day_slot UNIQUE (day_of_week, slot_number),
    CONSTRAINT ck_business_hours_day CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    CONSTRAINT ck_business_hours_slot CHECK (slot_number BETWEEN 1 AND 2),
    CONSTRAINT ck_business_hours_times CHECK ((closed = TRUE AND opens_at IS NULL AND closes_at IS NULL) OR (closed = FALSE AND opens_at IS NOT NULL AND closes_at IS NOT NULL AND closes_at > opens_at))
);
