-- 1546995564 UP add-column-has-pets-in-config-vehicle
ALTER TABLE config_vehicle
ADD COLUMN has_pets tinyint(1) NOT NULL DEFAULT 0;