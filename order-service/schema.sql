CREATE TABLE IF NOT EXISTS carts (
    cart_id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    order_id SERIAL PRIMARY KEY,
    order_date TIMESTAMP NOT NULL,
    order_desc VARCHAR(255),
    order_fee DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    cart_id INTEGER REFERENCES carts(cart_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS order_items (
    order_item_id SERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    order_id INTEGER REFERENCES orders(order_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_carts_user_id ON carts(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_cart_id ON orders(cart_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);