-- 1729731901 DOWN add parcel package tracking deleted
ALTER TABLE parcels_packages_tracking
MODIFY COLUMN action enum('register','paid','move','intransit','loaded','downloaded','incidence','canceled','closed','printed','delivered','deliveredcancel','located','arrived','createdlog','canceledlog','ead','rad','ready_to_transhipment','transhipped') DEFAULT NULL;