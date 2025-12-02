-- 1690034905 UP add-prepaid-price-km
ALTER TABLE pp_price_km
ADD COLUMN price_kg decimal(12,2),
ADD COLUMN price_cubic decimal(12,2),
ADD COLUMN R0 decimal(12,2),
ADD COLUMN R1 decimal(12,2),
ADD COLUMN R2 decimal(12,2),
ADD COLUMN R3 decimal(12,2),
ADD COLUMN R4 decimal(12,2),
ADD COLUMN R5 decimal(12,2),
ADD COLUMN R6 decimal(12,2),
ADD COLUMN R7 decimal(12,2);