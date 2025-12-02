-- 1557874438 UP add cancellation code column in parcels table
ALTER TABLE parcels
ADD COLUMN cancel_code varchar(30) DEFAULT NULL;