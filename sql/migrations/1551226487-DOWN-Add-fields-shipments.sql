-- 1551226487 DOWN Add fields shipments
ALTER TABLE shipments 
DROP FOREIGN KEY fk_shipments_shipments_id;
ALTER TABLE shipments 
DROP COLUMN right_stamp_status,
DROP COLUMN left_stamp_status,
DROP COLUMN parent_id,
DROP INDEX fk_shipments_shipments_id_idx ;