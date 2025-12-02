-- 1552421428 UP rental-iva-field
ALTER TABLE rental
ADD COLUMN iva decimal(12,2) NOT NULL DEFAULT 0.00;