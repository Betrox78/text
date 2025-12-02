-- 1686934633 UP add-package-measures-and-relationships-parcel-inhouse
CREATE TABLE package_measures(
	id INT PRIMARY KEY,
    weight DECIMAL(12, 2) NOT NULL,
    height DECIMAL(12, 2) NOT NULL,
    width DECIMAL(12, 2) NOT NULL,
    length DECIMAL(12, 2) NOT NULL
);

INSERT INTO package_measures
VALUES
(1, 40, 60, 70, 60),
(2, 40, 150, 70, 60),
(3, 130, 100, 110, 170);

ALTER TABLE package_price
MODIFY COLUMN shipping_type ENUM('parcel', 'courier', 'pets', 'frozen', 'inhouse') DEFAULT 'parcel' NOT NULL,
ADD COLUMN package_measures_id INT DEFAULT NULL AFTER shipping_type,
ADD CONSTRAINT fk_package_price_package_measures_id FOREIGN KEY (package_measures_id) REFERENCES package_measures(id) ON DELETE NO ACTION;

INSERT INTO package_price
(id, name_price, min_linear_volume, max_linear_volume, min_weight, max_weight, price, currency_id, shipping_type, created_by, min_m3, max_m3, package_measures_id)
VALUES
(11, 'RI-CAJA', 190, 190, 40, 40, 0, 22, 'inhouse', 1, 0, 0, 1),
(12, 'RI-ATADO', 280, 280, 40, 40, 0, 22, 'inhouse', 1, 0, 0, 2),
(13, 'RI-TARIMA', 380, 380, 130, 130, 0, 22, 'inhouse', 1, 0, 0, 3);

ALTER TABLE package_types
ADD COLUMN allowed_inhouse BOOLEAN DEFAULT FALSE AFTER shipping_type,
ADD COLUMN package_price_id INT DEFAULT NULL AFTER allowed_inhouse,
ADD CONSTRAINT fk_package_types_package_price_id FOREIGN KEY (package_price_id) REFERENCES package_price(id) ON DELETE NO ACTION;

UPDATE package_types SET allowed_inhouse = TRUE, package_price_id = 11 WHERE id = 1;
UPDATE package_types SET allowed_inhouse = TRUE, package_price_id = 12 WHERE id = 8;
UPDATE package_types SET allowed_inhouse = TRUE, package_price_id = 13 WHERE id = 3;

ALTER TABLE promos
MODIFY COLUMN service ENUM('boardingpass', 'parcel', 'rental', 'guiapp', 'parcel_inhouse') NOT NULL;