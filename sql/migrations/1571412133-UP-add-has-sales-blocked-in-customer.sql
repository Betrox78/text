-- 1571412133 UP add-has-sales-blocked-in-customer
ALTER TABLE customer
ADD COLUMN has_sales_blocked boolean DEFAULT false

