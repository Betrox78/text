-- 1711442259 DOWN add package measures parcel
DELETE FROM package_measures WHERE id IN (4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);

UPDATE package_price SET package_measures_id = NULL WHERE name_price = 'R0';
UPDATE package_price SET package_measures_id = NULL WHERE name_price = 'R1';
UPDATE package_price SET package_measures_id = NULL WHERE name_price = 'R2';
UPDATE package_price SET package_measures_id = NULL WHERE name_price = 'R3';
UPDATE package_price SET package_measures_id = NULL WHERE name_price = 'R4';
UPDATE package_price SET package_measures_id = NULL WHERE name_price = 'R5';
UPDATE package_price SET package_measures_id = NULL WHERE name_price = 'R6';
UPDATE package_price SET package_measures_id = NULL WHERE name_price = 'R7';
UPDATE package_price SET package_measures_id = NULL WHERE name_price = 'R81';
UPDATE package_price SET package_measures_id = NULL WHERE name_price = 'R82';
UPDATE package_price SET package_measures_id = NULL WHERE name_price = 'R83';
UPDATE package_price SET package_measures_id = NULL WHERE name_price = 'R8E';