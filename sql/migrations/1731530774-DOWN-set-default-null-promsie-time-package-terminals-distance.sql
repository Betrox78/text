-- 1731530774 DOWN set default null promsie time package terminals
UPDATE package_terminals_distance SET promise_time_ocu = '00:00', promise_time_ead = '00:00';

ALTER TABLE package_terminals_distance
MODIFY COLUMN promise_time_ocu VARCHAR(5) NOT NULL DEFAULT '00:00',
MODIFY COLUMN promise_time_ead VARCHAR(5) NOT NULL DEFAULT '00:00';

