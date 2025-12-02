-- 1732659637 DOWN add parcels package tracking actions RAD
ALTER TABLE parcels_packages_tracking
MODIFY COLUMN action ENUM('register','paid','move','intransit','loaded','downloaded','incidence','canceled','closed','printed','delivered','deliveredcancel','located','arrived','createdlog','canceledlog','ead','rad','ready_to_transhipment','transhipped','deleted') DEFAULT NULL;