-- 1690853080 UP parcels transhipments migrations
ALTER TABLE branchoffice
ADD receive_transhipments BOOLEAN NOT NULL DEFAULT FALSE AFTER manager_id,
ADD transhipment_site_name VARCHAR(30) DEFAULT NULL AFTER receive_transhipments;

ALTER TABLE parcels_packages_tracking
MODIFY COLUMN action ENUM('register', 'paid', 'move', 'intransit', 'loaded',
'downloaded', 'incidence', 'canceled', 'closed', 'printed', 'delivered',
'deliveredcancel', 'located', 'arrived', 'createdlog', 'canceledlog', 'ead', 'rad', 'ready_to_transhipment', 'transhipped');

CREATE TABLE parcels_transhipments(
	id INT(11) PRIMARY KEY AUTO_INCREMENT NOT NULL,
    parcel_id INT(11) NOT NULL,
    parcel_package_id INT(11) NOT NULL,
    count INT NOT NULL DEFAULT 1,
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by int(11) DEFAULT NULL,
	updated_at datetime DEFAULT NULL,
	updated_by int(11) DEFAULT NULL,
    KEY parcels_transhipments_parcel_id_idx (parcel_id),
	CONSTRAINT parcels_transhipments_parcel_id FOREIGN KEY (parcel_id) REFERENCES parcels(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
    KEY parcels_transhipments_parcel_package_id_idx (parcel_package_id),
	CONSTRAINT parcels_transhipments_parcel_package_id FOREIGN KEY (parcel_package_id) REFERENCES parcels_packages(id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE parcels_transhipments_history(
	id INT(11) PRIMARY KEY AUTO_INCREMENT NOT NULL,
    parcel_id INT(11) NOT NULL,
    parcel_package_id INT(11) NOT NULL,
    schedule_route_destination_id INT(11) NOT NULL,
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by int(11) DEFAULT NULL,
    KEY parcels_transhipments_history_parcel_id_idx (parcel_id),
	CONSTRAINT parcels_transhipments_history_parcel_id FOREIGN KEY (parcel_id) REFERENCES parcels(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
    KEY parcels_transhipments_history_parcel_package_id_idx (parcel_package_id),
	CONSTRAINT parcels_transhipments_history_parcel_package_id FOREIGN KEY (parcel_package_id) REFERENCES parcels_packages(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
    KEY parcels_transhipments_history_schedule_route_destination_id_idx (schedule_route_destination_id),
	CONSTRAINT parcels_transhipments_history_schedule_route_destination_id FOREIGN KEY (schedule_route_destination_id) REFERENCES schedule_route_destination(id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE branchoffice_parcel_receiving_config (
  id INT NOT NULL AUTO_INCREMENT,
  receiving_branchoffice_id INT NOT NULL,
  of_branchoffice_id INT NOT NULL,
  status TINYINT NOT NULL DEFAULT '1',
  created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT NULL,
  created_by int DEFAULT NULL,
  updated_by int DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_receiving_branchoffice_id FOREIGN KEY (receiving_branchoffice_id) REFERENCES branchoffice(id),
  CONSTRAINT fk_of_branchoffice_id FOREIGN KEY (of_branchoffice_id) REFERENCES branchoffice(id)
);