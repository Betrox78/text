-- 1731530774 UP set default null promsie time package terminals distance
ALTER TABLE package_terminals_distance
MODIFY COLUMN promise_time_ocu VARCHAR(5) DEFAULT NULL,
MODIFY COLUMN promise_time_ead VARCHAR(5) DEFAULT NULL;

UPDATE package_terminals_distance SET promise_time_ocu = NULL, promise_time_ead = NULL;