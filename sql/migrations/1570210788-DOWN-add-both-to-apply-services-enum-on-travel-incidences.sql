-- 1570210788 DOWN add-both-to-apply-services-enum-on-travel-incidences
ALTER TABLE travel_incidences
    MODIFY COLUMN apply_to_service ENUM('boarding_pass', 'rental') NOT NULL DEFAULT 'boarding_pass';