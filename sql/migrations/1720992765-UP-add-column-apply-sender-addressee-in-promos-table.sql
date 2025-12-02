-- 1720992765 UP add column apply sender addressee in promos table
ALTER TABLE promos
ADD apply_sender_addressee BOOLEAN NOT NULL DEFAULT FALSE AFTER apply_to_package_type;