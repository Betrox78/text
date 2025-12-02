-- 1543020463 UP add-cancel-reaso-rental

ALTER TABLE rental
ADD COLUMN cancel_reason varchar(254) DEFAULT NULL;