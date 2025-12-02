-- 1759283134 UP geolocation table
CREATE TABLE parcels_manifest_route_logs(
	id INT PRIMARY KEY AUTO_INCREMENT,
    parcel_manifest_id INT NOT NULL,
    parcel_manifest_detail_id INT DEFAULT NULL,
    speed FLOAT DEFAULT 0.0,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    type ENUM ('init', 'mark', 'delivery', 'delivery_attempt', 'end') NOT NULL DEFAULT 'mark',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT NOT NULL,
    CONSTRAINT fk_parcels_manifest_route_logs_parcel_manifest_id
		FOREIGN KEY (parcel_manifest_id) REFERENCES parcels_manifest(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT fk_parcels_manifest_route_logs_parcel_manifest_detail_id
		FOREIGN KEY (parcel_manifest_detail_id) REFERENCES parcels_manifest_detail(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT fk_parcels_manifest_route_logs_created_by
		FOREIGN KEY (created_by) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE NO ACTION
);
CREATE INDEX idx_parcels_route_detail_type ON parcels_manifest_route_logs(parcel_manifest_id, parcel_manifest_detail_id, type);