-- 1574359769 UP insert-county-cities-and-update-suburbs
INSERT INTO county(id, name, state_id) VALUES(2329, 'Álvaro Obregón', 22);
INSERT INTO county(id, name, state_id) VALUES(2330, 'Venustiano Carranza', 22);

INSERT INTO city(id, name, county_id) VALUES(705, 'Ciudad de México', 2326);
INSERT INTO city(id, name, county_id) VALUES(706, 'Ciudad de México', 2330);
UPDATE city SET county_id = 2329 WHERE id = 561;

UPDATE suburb SET county_id = 2330 where zip_code between 15000 and 15990;
UPDATE suburb SET county_id = 2329 where zip_code between 01000 and 01904;