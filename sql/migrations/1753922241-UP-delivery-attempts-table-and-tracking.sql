-- 1753922241 UP delivery attempts table and tracking
CREATE TABLE IF NOT EXISTS parcels_delivery_attempts(
	id INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
    parcel_id INTEGER NOT NULL,
    parcel_manifest_detail_id INTEGER NOT NULL,
    delivery_attempt_reason_id INTEGER NOT NULL,
    notes VARCHAR(255) DEFAULT '',
    image_name VARCHAR(255) DEFAULT NULL,
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by INTEGER NOT NULL,
    KEY parcels_delivery_attempts_parcel_id_idx (parcel_id),
	CONSTRAINT parcels_delivery_attempts_parcel_id FOREIGN KEY (parcel_id) REFERENCES parcels(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
    KEY parcels_delivery_attempts_parcel_manifest_detail_id_idx (parcel_manifest_detail_id),
	CONSTRAINT parcels_delivery_attempts_parcel_manifest_detail_id FOREIGN KEY (parcel_manifest_detail_id) REFERENCES parcels_manifest_detail(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
    KEY parcels_delivery_attempts_delivery_attempt_reason_id_idx (delivery_attempt_reason_id),
	CONSTRAINT parcels_delivery_attempts_delivery_attempt_reason_id FOREIGN KEY (delivery_attempt_reason_id) REFERENCES delivery_attempt_reason(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
    KEY parcels_delivery_attempts_created_by_idx (created_by),
	CONSTRAINT parcels_delivery_attempts_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

ALTER TABLE parcels_packages_tracking
MODIFY COLUMN action enum('register','paid','move','intransit','loaded','downloaded','incidence','canceled','closed','printed','delivered','deliveredcancel','located','arrived','createdlog','canceledlog','ead','rad','ready_to_transhipment','transhipped','deleted','pending_collection','collecting','collected','in_origin','delivery_attempt') DEFAULT NULL;