-- 1675818591 DOWN add-distance-terminal-parcel-packages
DELETE FROM package_terminals_distance WHERE terminal_origin_id = 16 AND terminal_destiny_id = 14;
DELETE FROM package_terminals_distance WHERE terminal_origin_id = 14 AND terminal_destiny_id = 17;