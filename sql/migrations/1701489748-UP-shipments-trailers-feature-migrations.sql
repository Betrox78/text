-- 1701489748 UP shipments-trailers-feature-migrations
CREATE TABLE trailers(
	id INT(11) PRIMARY KEY AUTO_INCREMENT,
    c_SubTipoRem_id INT(11) NOT NULL,
    plate VARCHAR(30) NOT NULL,
    status TINYINT(4) NOT NULL DEFAULT 1,
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by INT(11) NOT NULL,
	updated_at DATETIME NULL DEFAULT NULL,
	updated_by INT(11) NULL DEFAULT NULL,
    CONSTRAINT fk_c_SubTipoRem_id FOREIGN KEY (c_SubTipoRem_id) REFERENCES c_SubTipoRem(id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE shipments_trailers(
	id INT(11) PRIMARY KEY AUTO_INCREMENT,
    shipment_id INT(11) NOT NULL,
    trailer_id INT(11) NOT NULL,
    status TINYINT(4) NOT NULL DEFAULT 1,
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by INT(11) NOT NULL,
	updated_at DATETIME NULL DEFAULT NULL,
	updated_by INT(11) NULL DEFAULT NULL,
    CONSTRAINT fk_shipment_id FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
	CONSTRAINT fk_trailer_id FOREIGN KEY (trailer_id) REFERENCES trailers(id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

ALTER TABLE shipments_parcel_package_tracking
MODIFY COLUMN status ENUM('loaded', 'ready-to-go', 'in-transit', 'arrived-to-terminal', 'downloaded', 'transfer') NOT NULL,
ADD COLUMN trailer_id INT(11) DEFAULT NULL AFTER shipment_id;