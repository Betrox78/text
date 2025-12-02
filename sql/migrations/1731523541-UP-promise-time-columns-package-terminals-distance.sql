-- 1731523541 UP promise-time-columns-package-terminals-distance
ALTER TABLE package_terminals_distance
ADD COLUMN promise_time_ocu VARCHAR(5) NOT NULL DEFAULT '00:00' AFTER distance_km,
ADD COLUMN promise_time_ead VARCHAR(5) NOT NULL DEFAULT '00:00' AFTER promise_time_ocu;