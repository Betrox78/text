-- 1542476261 UP add-destiny-name
ALTER TABLE rental
ADD COLUMN destiny_city_name varchar(100) DEFAULT NULL AFTER destiny_city_id;

UPDATE rental AS r
SET
    r.destiny_city_name = (SELECT
            c.name
        FROM
            city AS c
        WHERE
            id = r.destiny_city_id
        LIMIT 1)
WHERE
    r.destiny_city_id IS NOT NULL;