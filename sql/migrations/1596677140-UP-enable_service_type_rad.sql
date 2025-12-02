-- 1596677140 UP enable_service_type_rad
UPDATE `parcel_service_type` SET `type_service` = 'RAD/OCU' WHERE (`type_service` = 'RAD');
UPDATE `parcel_service_type` SET `type_service` = 'RAD/EAD' WHERE (`type_service` = 'RAD-EAD');
UPDATE `parcel_service_type` SET `status` = '1' WHERE (`type_service` = 'RAD/OCU');