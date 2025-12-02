-- 1574213171 UP add-new-countys-and-update-suburbs
INSERT INTO county(id, name, state_id) VALUES(2323, 'Benito Juárez', 6); -- Veracruz
INSERT INTO county(id, name, state_id) VALUES(2324, 'Benito Juárez', 8); -- Quintana Roo
INSERT INTO county(id, name, state_id) VALUES(2325, 'Benito Juárez', 19); -- Guerrero
INSERT INTO county(id, name, state_id) VALUES(2326, 'Benito Juárez', 22); -- CDMX
INSERT INTO county(id, name, state_id) VALUES(2327, 'Benito Juárez', 32); -- Zacatecas
INSERT INTO county(id, name, state_id) VALUES(2328, 'Benito Juárez', 33); -- Tlaxcala

UPDATE suburb SET county_id = 2323 WHERE id BETWEEN 20176 AND 20193;
UPDATE suburb SET county_id = 2324 WHERE id BETWEEN 33090 AND 33412;
UPDATE suburb SET county_id = 2325 WHERE id BETWEEN 88975 AND 88994;
UPDATE suburb SET county_id = 2326 WHERE id BETWEEN 106897 AND 106950;
UPDATE suburb SET county_id = 2327 WHERE id BETWEEN 137291 AND 137308;
UPDATE suburb SET county_id = 2328 WHERE id = 137760;

UPDATE customer_billing_information SET county_id = 2326 where id = 90;
UPDATE customer_billing_information SET county_id = 2326 where id = 195;