-- 1703818926 UP migration assign action shipments trailers
ALTER TABLE shipments_trailers
MODIFY COLUMN action ENUM('assign', 'hitch', 'release', 'transfer', 'release_transhipment') AFTER latest_movement;