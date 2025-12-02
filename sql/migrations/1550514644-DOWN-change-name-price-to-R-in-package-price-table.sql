-- 1550514644 DOWN change-name-price-to-R-in-package-price-table
UPDATE package_price SET name_price = 'T0' WHERE name_price = 'R0';
UPDATE package_price SET name_price = 'T1' WHERE name_price = 'R1';
UPDATE package_price SET name_price = 'T2' WHERE name_price = 'R2';
UPDATE package_price SET name_price = 'T3' WHERE name_price = 'R3';
UPDATE package_price SET name_price = 'T4' WHERE name_price = 'R4';
UPDATE package_price SET name_price = 'T5' WHERE name_price = 'R5';
UPDATE package_price SET name_price = 'TS' WHERE name_price = 'RS';