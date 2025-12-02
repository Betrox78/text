-- 1686934633 DOWN add-package-measures-and-relationships-parcel-inhouse
ALTER TABLE package_price
MODIFY COLUMN shipping_type ENUM('parcel', 'courier', 'pets', 'frozen') DEFAULT 'parcel' NOT NULL,
DROP FOREIGN KEY fk_package_price_package_measures_id,
DROP COLUMN package_measures_id;

DROP TABLE package_measures;

DELETE FROM package_price WHERE id = 11;
DELETE FROM package_price WHERE id = 12;
DELETE FROM package_price WHERE id = 13;

ALTER TABLE package_types
DROP COLUMN allowed_inhouse,
DROP FOREIGN KEY fk_package_types_package_price_id,
DROP COLUMN package_price_id;

ALTER TABLE promos
MODIFY COLUMN service ENUM('boardingpass', 'parcel', 'rental', 'guiapp') NOT NULL;