-- 1716343708 UP breakdown columns packages and packings
ALTER TABLE parcels_packages
ADD COLUMN excess_promo_id INTEGER DEFAULT NULL AFTER promo_id,
ADD COLUMN iva DECIMAL(12,2) DEFAULT 0.00 NOT NULL AFTER discount,
ADD COLUMN excess_discount DECIMAL(12,2) DEFAULT 0.00 NOT NULL AFTER iva,
ADD CONSTRAINT fk_parcels_packages_excess_promo_id FOREIGN KEY (excess_promo_id) REFERENCES promos(id);

ALTER TABLE parcels_packings
ADD COLUMN iva DECIMAL(12,2) DEFAULT 0.00 NOT NULL AFTER discount;