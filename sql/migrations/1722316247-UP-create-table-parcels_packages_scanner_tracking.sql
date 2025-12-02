-- 1722316247 UP create table parcels_packages_scanner_tracking
CREATE TABLE parcels_packages_scanner_tracking (
	id INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
    parcel_id INT NOT NULL,
    parcel_package_id INT DEFAULT NULL,
    schedule_route_id INT DEFAULT NULL,
    branchoffice_id INT DEFAULT NULL,
    trailer_id INT DEFAULT NULL,
    message VARCHAR(255) NOT NULL,
    action ENUM('load', 'download'),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT NOT NULL,
    CONSTRAINT parcels_packages_scanner_tracking_parcel_id_tk FOREIGN KEY (parcel_id)
		REFERENCES parcels(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT parcels_packages_scanner_tracking_parcel_package_id_tk FOREIGN KEY (parcel_package_id)
		REFERENCES parcels_packages(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT parcels_packages_scanner_tracking_schedule_route_id_tk FOREIGN KEY (schedule_route_id)
		REFERENCES schedule_route(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT parcels_packages_scanner_tracking_branchoffice_id_tk FOREIGN KEY (branchoffice_id)
		REFERENCES branchoffice(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT parcels_packages_scanner_tracking_trailer_id_tk FOREIGN KEY (trailer_id)
		REFERENCES trailers(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
	CONSTRAINT parcels_packages_scanner_tracking_created_by_tk FOREIGN KEY (created_by)
		REFERENCES users(id) ON UPDATE NO ACTION ON DELETE NO ACTION
);