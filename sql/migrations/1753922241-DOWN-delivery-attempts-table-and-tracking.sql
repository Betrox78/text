-- 1753922241 DOWN delivery attempts table and tracking
DROP TABLE parcels_delivery_attempts;

ALTER TABLE parcels_packages_tracking
MODIFY COLUMN action enum('register','paid','move','intransit','loaded','downloaded','incidence','canceled','closed','printed','delivered','deliveredcancel','located','arrived','createdlog','canceledlog','ead','rad','ready_to_transhipment','transhipped','deleted','pending_collection','collecting','collected','in_origin') DEFAULT NULL;