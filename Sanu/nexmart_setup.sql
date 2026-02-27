
CREATE DATABASE IF NOT EXISTS nexmart;
USE nexmart;

-- Users
CREATE TABLE IF NOT EXISTS Users (
    user_id  INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);
INSERT IGNORE INTO Users (username, password) VALUES ('admin', 'admin123');

-- Suppliers
CREATE TABLE IF NOT EXISTS Suppliers (
    supplier_id INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    contact     VARCHAR(50),
    address     TEXT
);

-- Products
CREATE TABLE IF NOT EXISTS Products (
    product_id        INT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    price             DECIMAL(10,2) NOT NULL,
    stock             INT NOT NULL DEFAULT 0,
    category          VARCHAR(50),
    supplier_id       INT,
    reorder_threshold INT DEFAULT 10,
    FOREIGN KEY (supplier_id) REFERENCES Suppliers(supplier_id) ON DELETE SET NULL
);

-- Customers
CREATE TABLE IF NOT EXISTS Customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100),
    contact     VARCHAR(50)
);

-- Sales
CREATE TABLE IF NOT EXISTS Sales (
    sale_id     INT AUTO_INCREMENT PRIMARY KEY,
    sale_date   DATE NOT NULL,
    customer_id INT,
    total       DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES Customers(customer_id) ON DELETE SET NULL
);

-- SaleItems
CREATE TABLE IF NOT EXISTS SaleItems (
    item_id    INT AUTO_INCREMENT PRIMARY KEY,
    sale_id    INT,
    product_id INT,
    quantity   INT NOT NULL,
    price      DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (sale_id)    REFERENCES Sales(sale_id)       ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES Products(product_id) ON DELETE SET NULL
);

-- Sample data
INSERT IGNORE INTO Suppliers (name, contact, address) VALUES
  ('Vista Wholesale', '0771112222', '10 Harbor St, Colombo'),
  ('Pinnacle Goods',  '0779998888', '88 Lake Drive, Kandy');

INSERT IGNORE INTO Products (name, price, stock, category, supplier_id, reorder_threshold) VALUES
  ('Basmati Rice (kg)', 180.00,  90, 'Groceries',   1, 20),
  ('Cane Sugar (kg)',   220.00,  55, 'Groceries',   1, 15),
  ('Extra Virgin Oil',  420.00,   7, 'Groceries',   2, 10),
  ('A4 Copy Paper',     580.00,  30, 'Stationery',  2, 10),
  ('Herbal Soap',        95.00,   3, 'Household',   1,  5),
  ('Green Tea (box)',   310.00,  18, 'Beverages',   2,  8);
