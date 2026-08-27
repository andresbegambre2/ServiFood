CREATE TABLE ingredients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    stock_current DECIMAL(14,3) NOT NULL DEFAULT 0,
    stock_minimum DECIMAL(14,3) NOT NULL DEFAULT 0,
    unit_cost DECIMAL(14,4),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_ingredients PRIMARY KEY (id),
    CONSTRAINT uk_ingredients_name UNIQUE (name),
    CONSTRAINT ck_ingredients_unit CHECK (unit IN ('GRAM','MILLILITER','UNIT')),
    CONSTRAINT ck_ingredients_stock CHECK (stock_current >= 0 AND stock_minimum >= 0),
    CONSTRAINT ck_ingredients_cost CHECK (unit_cost IS NULL OR unit_cost >= 0)
);

CREATE TABLE product_recipe_ingredients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    quantity DECIMAL(14,3) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_product_recipe_ingredients PRIMARY KEY (id),
    CONSTRAINT uk_product_recipe_ingredient UNIQUE (product_id, ingredient_id),
    CONSTRAINT fk_product_recipe_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_recipe_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients(id),
    CONSTRAINT ck_product_recipe_quantity CHECK (quantity > 0)
);

CREATE TABLE extra_recipe_ingredients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    extra_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    quantity DECIMAL(14,3) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_extra_recipe_ingredients PRIMARY KEY (id),
    CONSTRAINT uk_extra_recipe_ingredient UNIQUE (extra_id, ingredient_id),
    CONSTRAINT fk_extra_recipe_extra FOREIGN KEY (extra_id) REFERENCES extras(id) ON DELETE CASCADE,
    CONSTRAINT fk_extra_recipe_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients(id),
    CONSTRAINT ck_extra_recipe_quantity CHECK (quantity > 0)
);

ALTER TABLE orders ADD COLUMN inventory_consumed_at TIMESTAMP(6);
ALTER TABLE orders ADD COLUMN inventory_reverted_at TIMESTAMP(6);

CREATE TABLE inventory_movements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ingredient_id BIGINT NOT NULL,
    order_id BIGINT,
    created_by BIGINT,
    movement_type VARCHAR(20) NOT NULL,
    quantity_delta DECIMAL(14,3) NOT NULL,
    balance_after DECIMAL(14,3) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_inventory_movements PRIMARY KEY (id),
    CONSTRAINT fk_inventory_movement_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients(id),
    CONSTRAINT fk_inventory_movement_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_inventory_movement_user FOREIGN KEY (created_by) REFERENCES internal_users(id),
    CONSTRAINT uk_inventory_order_ingredient_type UNIQUE (order_id, ingredient_id, movement_type),
    CONSTRAINT ck_inventory_movement_type CHECK (movement_type IN ('ENTRY','CONSUMPTION','ADJUSTMENT','REVERSAL')),
    CONSTRAINT ck_inventory_movement_delta CHECK (quantity_delta <> 0),
    CONSTRAINT ck_inventory_movement_balance CHECK (balance_after >= 0)
);

CREATE INDEX ix_ingredients_alerts ON ingredients(active, stock_current, stock_minimum);
CREATE INDEX ix_product_recipe_product ON product_recipe_ingredients(product_id);
CREATE INDEX ix_extra_recipe_extra ON extra_recipe_ingredients(extra_id);
CREATE INDEX ix_inventory_movements_created ON inventory_movements(created_at);
CREATE INDEX ix_inventory_movements_ingredient ON inventory_movements(ingredient_id, created_at);
CREATE INDEX ix_inventory_movements_order ON inventory_movements(order_id);
