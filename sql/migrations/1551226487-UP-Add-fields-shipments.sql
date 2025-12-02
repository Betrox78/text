-- 1551226487 UP Add fields shipments
ALTER TABLE shipments
ADD COLUMN parent_id INT(11) NULL AFTER total_packages,
ADD COLUMN left_stamp_status INT(11) NOT NULL DEFAULT 1 AFTER parent_id,
ADD COLUMN right_stamp_status INT(11) NOT NULL DEFAULT 1 AFTER left_stamp_status,
ADD INDEX fk_shipments_shipments_id_idx (parent_id ASC);

ALTER TABLE shipments 
ADD CONSTRAINT fk_shipments_shipments_id
  FOREIGN KEY (parent_id)
  REFERENCES shipments (id)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;