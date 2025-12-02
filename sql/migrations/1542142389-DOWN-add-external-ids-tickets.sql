-- 1542142389 DOWN add-external-ids-tickets
ALTER TABLE tickets
DROP FOREIGN KEY `tickets_boarding_pass_id_fk`;
ALTER TABLE tickets
DROP COLUMN `boarding_pass_id`,
DROP INDEX `tickets_boarding_pass_id_fk`;

ALTER TABLE tickets
DROP FOREIGN KEY `tickets_rental_id_fk`;
ALTER TABLE tickets
DROP COLUMN `rental_id`,
DROP INDEX `tickets_rental_id_fk`;

ALTER TABLE tickets
DROP FOREIGN KEY `tickets_parcel_id_fk`;
ALTER TABLE tickets
DROP COLUMN `parcel_id`,
DROP INDEX `tickets_parcel_id_fk`;

ALTER TABLE tickets
DROP COLUMN `action`;

