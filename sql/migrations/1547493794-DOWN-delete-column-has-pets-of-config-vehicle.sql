-- 1547493794 DOWN delete-column-has-pets-of-config-vehicle
ALTER TABLE config_vehicle
ADD COLUMN has_pets tinyint(1) NOT NULL DEFAULT 0;