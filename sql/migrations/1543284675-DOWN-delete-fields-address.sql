-- 1543284675 DOWN delete-fields-address
ALTER TABLE parcels
ADD COLUMN sender_address varchar(254) NOT NULL AFTER sender_zip_code,
ADD COLUMN addressee_address varchar(254) NOT NULL AFTER addressee_zip_code;