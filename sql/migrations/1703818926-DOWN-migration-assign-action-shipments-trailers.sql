-- 1703818926 DOWN migration assign action shipments trailers
ALTER TABLE shipments_trailers
MODIFY COLUMN action ENUM('hitch', 'release', 'transfer', 'release_transhipment') AFTER latest_movement;