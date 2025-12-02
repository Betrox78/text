-- 1542142389 UP add-external-ids-tickets

ALTER TABLE tickets
ADD COLUMN boarding_pass_id int(11) DEFAULT NULL AFTER was_printed,
ADD COLUMN rental_id int(11) DEFAULT NULL AFTER boarding_pass_id,
ADD COLUMN parcel_id int(11) DEFAULT NULL AFTER rental_id,
ADD COLUMN action enum('purchase', 'income', 'charge', 'cancel', 'expense', 'withdrawal') DEFAULT 'purchase' AFTER parcel_id;

ALTER TABLE tickets
ADD CONSTRAINT tickets_boarding_pass_id_fk
  FOREIGN KEY (boarding_pass_id)
  REFERENCES boarding_pass (id);

ALTER TABLE tickets
ADD CONSTRAINT tickets_rental_id_fk
  FOREIGN KEY (rental_id)
  REFERENCES rental (id);

ALTER TABLE tickets
ADD CONSTRAINT tickets_parcel_id_fk
  FOREIGN KEY (parcel_id)
  REFERENCES parcels (id);

