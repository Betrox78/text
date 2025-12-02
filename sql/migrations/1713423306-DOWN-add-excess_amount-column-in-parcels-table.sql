-- 1713423306 DOWN add excess_amount column in parcels table
ALTER TABLE parcels
DROP COLUMN excess_amount,
DROP COLUMN excess_discount;