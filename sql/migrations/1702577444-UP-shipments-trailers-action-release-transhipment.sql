-- 1702577444 UP shipments trailers action release transhipment
ALTER TABLE shipments_trailers
MODIFY COLUMN action ENUM('hitch', 'release', 'transfer', 'release_transhipment');