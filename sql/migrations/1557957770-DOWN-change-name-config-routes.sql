-- 1557957770 DOWN change-name-config-routes

ALTER TABLE config_route
MODIFY COLUMN name varchar(55) NOT NULL;