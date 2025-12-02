-- 1724735970 UP add column notes invoice in parcels table
ALTER TABLE parcels
ADD COLUMN notes_invoice VARCHAR(255) DEFAULT NULL AFTER notes;