-- 1561765428 UP add discount per base column in promos table
ALTER TABLE promos
ADD COLUMN discount_per_base tinyint(1) NOT NULL DEFAULT 0 AFTER has_specific_customer;