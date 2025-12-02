-- 1543095661 UP add excess fields in parcels_packages and foreign key to excess_price_id
ALTER TABLE parcels_packages
ADD COLUMN excess_price_id int(11) DEFAULT NULL,
ADD CONSTRAINT fk_excess_price_id FOREIGN KEY (excess_price_id) REFERENCES package_price(id) ON DELETE CASCADE,
ADD COLUMN excess_price decimal(12,2) DEFAULT NULL,
ADD COLUMN excess_cost decimal(12,2) DEFAULT NULL;