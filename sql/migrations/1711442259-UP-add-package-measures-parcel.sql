-- 1711442259 UP add package measures parcel
INSERT INTO package_measures(id, weight, height, width, length)
VALUES
(4, .09, 20, 20, 20), -- r0
(5, 10, 30, 30, 30), -- r1
(6, 20, 38, 38, 39), -- r2
(7, 30, 47, 46, 46), -- r3
(8, 40, 55, 54, 54), -- r4
(9, 50, 63, 62, 62), -- r5
(10, 60, 68, 68, 69), -- r6
(11, 70, 68, 69, 69), -- r7
(12, 400, 90, 90, 90), -- r81
(13, 800, 113, 113, 113), -- r82
(14, 1400, 129, 129, 130), -- r83
(15, 5000, 464, 464, 464); -- r8E

UPDATE package_price SET package_measures_id = 4 WHERE name_price = 'R0';
UPDATE package_price SET package_measures_id = 5 WHERE name_price = 'R1';
UPDATE package_price SET package_measures_id = 6 WHERE name_price = 'R2';
UPDATE package_price SET package_measures_id = 7 WHERE name_price = 'R3';
UPDATE package_price SET package_measures_id = 8 WHERE name_price = 'R4';
UPDATE package_price SET package_measures_id = 9 WHERE name_price = 'R5';
UPDATE package_price SET package_measures_id = 10 WHERE name_price = 'R6';
UPDATE package_price SET package_measures_id = 11 WHERE name_price = 'R7';
UPDATE package_price SET package_measures_id = 12 WHERE name_price = 'R81';
UPDATE package_price SET package_measures_id = 13 WHERE name_price = 'R82';
UPDATE package_price SET package_measures_id = 14 WHERE name_price = 'R83';
UPDATE package_price SET package_measures_id = 15 WHERE name_price = 'R8E';