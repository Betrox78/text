-- 1543282380 UP add-sender-and-addressee-address-id
ALTER TABLE parcels
ADD COLUMN sender_address_id int(11) DEFAULT NULL AFTER sender_zip_code,
ADD CONSTRAINT fk_sender_address_id FOREIGN KEY (sender_address_id) REFERENCES customer_addresses(id) ON DELETE CASCADE,
ADD COLUMN addressee_address_id int(11) DEFAULT NULL AFTER addressee_zip_code,
ADD CONSTRAINT fk_addressee_address_id FOREIGN KEY (addressee_address_id) REFERENCES customer_addresses(id) ON DELETE CASCADE;