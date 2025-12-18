-- Удаляем старые таблицы если существуют
DROP TABLE IF EXISTS product_photo CASCADE;
DROP TABLE IF EXISTS product_categories CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS categories CASCADE;

-- Таблица категорий
CREATE TABLE categories (
    category_id SERIAL PRIMARY KEY,
    category_title VARCHAR(255) NOT NULL,
    parent_category_id INTEGER,
    FOREIGN KEY (parent_category_id) REFERENCES categories(category_id) ON DELETE SET NULL
);

-- Таблица продуктов
CREATE TABLE products (
    product_id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    quantity INTEGER NOT NULL DEFAULT 0,
    quantity_status VARCHAR(50) NOT NULL,
    price_unit DECIMAL(10,2) NOT NULL,
    discount BIGINT,
    version INTEGER DEFAULT 0
);

-- Связующая таблица продуктов и категорий
CREATE TABLE product_categories (
    product_id BIGINT NOT NULL,
    category_id INTEGER NOT NULL,
    PRIMARY KEY (product_id, category_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE CASCADE
);

-- Таблица фотографий продуктов
CREATE TABLE product_photo (
    photo_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    photo_link TEXT NOT NULL,
    original_file_name VARCHAR(255),
    file_size BIGINT,
    content_type VARCHAR(100),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

-- Индексы для производительности
CREATE INDEX idx_categories_parent_id ON categories(parent_category_id);
CREATE INDEX idx_products_quantity ON products(quantity);
CREATE INDEX idx_product_categories_product_id ON product_categories(product_id);
CREATE INDEX idx_product_categories_category_id ON product_categories(category_id);
CREATE INDEX idx_product_photo_product_id ON product_photo(product_id);