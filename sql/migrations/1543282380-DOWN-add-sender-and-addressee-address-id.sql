-- 1543282380 DOWN add-sender-and-addressee-address-id
ALTER TABLE parcels
DROP FOREIGN KEY fk_sender_address_id,
DROP FOREIGN KEY fk_addressee_address_id,
DROP COLUMN sender_address_id,
DROP COLUMN addressee_address_id;