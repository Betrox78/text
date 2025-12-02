-- 1570210788 UP add-both-to-apply-services-enum-on-travel-incidences
ALTER TABLE travel_incidences
    MODIFY COLUMN apply_to_service ENUM('boarding_pass', 'rental', 'both') NOT NULL DEFAULT 'boarding_pass';