-- 1553199139 UP add action value deliveredcancel in parcels packages tracking table
ALTER TABLE parcels_packages_tracking
MODIFY COLUMN action enum('register','paid','move','intransit','loaded','downloaded','incidence','canceled','closed','printed','delivered','deliveredcancel') NOT NULL AFTER ticket_id;