-- 1571947358 UP is promo customer column was added in customers promos
ALTER TABLE customers_promos
ADD COLUMN is_promo_customer TINYINT(1) NOT NULL DEFAULT 1 AFTER customer_id;