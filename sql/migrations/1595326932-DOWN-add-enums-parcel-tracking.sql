-- 1595326932 DOWN add-enums-parcel-tracking
ALTER TABLE parcels_packages_tracking DROP COLUMN action;
ALTER TABLE parcels_packages_tracking ADD action  enum('register','paid','move','intransit','loaded','downloaded','incidence','canceled','closed','printed','delivered','deliveredcancel','located','arrived') NOT NULL
