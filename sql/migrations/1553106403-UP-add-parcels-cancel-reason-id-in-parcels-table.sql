-- 1553106403 UP add parcels cancel reason id in parcels table
ALTER TABLE parcels
ADD COLUMN parcels_cancel_reason_id int(11) DEFAULT NULL,
ADD KEY parcels_parcels_cancel_reason_id_idx (parcels_cancel_reason_id),
ADD CONSTRAINT parcels_parcels_cancel_reason_id_fk FOREIGN KEY (parcels_cancel_reason_id) REFERENCES parcels_cancel_reasons (id) ON DELETE SET NULL ON UPDATE NO ACTION;