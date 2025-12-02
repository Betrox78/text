-- 1571943947 UP customer limit column was added in promos table
ALTER TABLE promos
ADD COLUMN customer_limit INT(11) NOT NULL DEFAULT 0 AFTER available_days;