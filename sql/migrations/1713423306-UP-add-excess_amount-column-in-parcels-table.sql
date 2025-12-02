-- 1713423306 UP add excess_amount column in parcels table
ALTER TABLE parcels
ADD COLUMN excess_amount DECIMAL(12,2) DEFAULT 0.00 NOT NULL AFTER discount,
ADD COLUMN excess_discount DECIMAL(12,2) DEFAULT 0.00 NOT NULL AFTER excess_amount;