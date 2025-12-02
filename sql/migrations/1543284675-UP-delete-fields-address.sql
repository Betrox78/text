-- 1543284675 UP delete-fields-address
ALTER TABLE parcels
DROP COLUMN sender_address,
DROP COLUMN addressee_address;