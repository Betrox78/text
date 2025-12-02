-- 1553199139 DOWN add action value deliveredcancel in parcels packages tracking table
ALTER TABLE parcels_packages_tracking
MODIFY COLUMN action enum('register','paid','move','intransit','loaded','downloaded','incidence','canceled','closed','printed','delivered') NOT NULL AFTER ticket_id;