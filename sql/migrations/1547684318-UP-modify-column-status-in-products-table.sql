-- 1547684318 UP modify-column-status-in-products-table
ALTER TABLE products
MODIFY COLUMN status int(11) DEFAULT 1 NOT NULL;