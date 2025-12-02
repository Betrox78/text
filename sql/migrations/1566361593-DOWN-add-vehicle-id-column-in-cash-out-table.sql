-- 1566361593 DOWN add-vehicle id column in cash out table
ALTER TABLE cash_out
DROP FOREIGN KEY fk_cash_out_vehicle_id,
DROP COLUMN vehicle_id;