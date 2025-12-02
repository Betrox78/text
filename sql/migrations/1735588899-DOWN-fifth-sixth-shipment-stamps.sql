-- 1735588899 DOWN fifth sixth shipment stamps
ALTER TABLE shipments
DROP COLUMN fifth_stamp,
DROP COLUMN sixth_stamp,
DROP COLUMN second_fifth_stamp,
DROP COLUMN second_sixth_stamp;