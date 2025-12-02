-- 1574359769 DOWN insert-county-cities-and-update-suburbs
UPDATE suburb SET county_id = 1333 WHERE zip_code BETWEEN 15000 AND 15990;
UPDATE suburb SET county_id = 451 WHERE zip_code BETWEEN 01000 AND 01904;
UPDATE city SET county_id = 1333 WHERE id = 561;

DELETE FROM city WHERE id BETWEEN 705 AND 706;
DELETE FROM county WHERE id BETWEEN 2329 AND 2330;