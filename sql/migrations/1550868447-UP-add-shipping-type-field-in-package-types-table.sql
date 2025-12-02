-- 1550868447 UP add-shipping-type-field-in-package-types-table
ALTER TABLE package_types
ADD COLUMN shipping_type enum('parcel', 'courier', 'pets', 'frozen') NOT NULL DEFAULT 'parcel' AFTER name;