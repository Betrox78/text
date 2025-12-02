-- 1735588899 UP fifth sixth shipment stamps
ALTER TABLE shipments
ADD COLUMN fifth_stamp INTEGER DEFAULT NULL AFTER replacement_stamp,
ADD COLUMN sixth_stamp INTEGER DEFAULT NULL AFTER fifth_stamp,
ADD COLUMN second_fifth_stamp INTEGER DEFAULT NULL AFTER second_replacement_stamp,
ADD COLUMN second_sixth_stamp INTEGER DEFAULT NULL AFTER second_fifth_stamp;