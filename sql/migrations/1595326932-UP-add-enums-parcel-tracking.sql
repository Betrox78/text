-- 1595326932 UP add-enums-parcel-tracking
ALTER TABLE parcels_packages_tracking
MODIFY COLUMN
action enum('register','paid','move','intransit','loaded','downloaded','incidence','canceled','closed','printed','delivered','deliveredcancel','located','arrived', 'createdlog','canceledlog','ead')