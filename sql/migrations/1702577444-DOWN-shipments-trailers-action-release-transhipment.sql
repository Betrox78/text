-- 1702577444 DOWN shipments trailers action release transhipment
ALTER TABLE shipments_trailers
MODIFY COLUMN action ENUM('hitch', 'release', 'transfer');