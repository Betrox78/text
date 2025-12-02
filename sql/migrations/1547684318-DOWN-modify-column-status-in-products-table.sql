-- 1547684318 DOWN modify-column-status-in-products-table
ALTER TABLE products
MODIFY COLUMN status tinyint(1) DEFAULT 0 NOT NULL;