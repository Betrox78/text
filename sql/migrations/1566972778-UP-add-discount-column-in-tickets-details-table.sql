-- 1566972778 UP add discount column in tickets details table
ALTER TABLE tickets_details
ADD COLUMN discount decimal(12,2) NOT NULL DEFAULT 0.00 AFTER unit_price;