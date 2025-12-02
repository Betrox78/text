-- 1574213171 DOWN add-new-countys-and-update-suburbs
UPDATE suburb SET county_id = 74 WHERE id BETWEEN 20176 AND 20193;
UPDATE suburb SET county_id = 74 WHERE id BETWEEN 33090 AND 33412;
UPDATE suburb SET county_id = 74 WHERE id BETWEEN 88975 AND 88994;
UPDATE suburb SET county_id = 74 WHERE id BETWEEN 106897 AND 106950;
UPDATE suburb SET county_id = 74 WHERE id BETWEEN 137291 AND 137308;
UPDATE suburb SET county_id = 74 WHERE id = 137760;

UPDATE customer_billing_information SET county_id = 74 where id = 90;
UPDATE customer_billing_information SET county_id = 74 where id = 195;

DELETE FROM county WHERE id BETWEEN 2323 AND 2328;