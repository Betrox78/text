-- 1588290320 UP create-table-coverage-parcel-coverage
CREATE TABLE IF NOT EXISTS parcel_coverage(
  id int(11) NOT NULL AUTO_INCREMENT,
  status tinyint(4) NOT NULL DEFAULT 1,
  suburb_id int(11) NOT NULL,
  zip_code int(11) NOT NULL,
  branchoffice_id int(11) NOT NULL,
  created_at datetime NULL DEFAULT CURRENT_TIMESTAMP,
  created_by int(11) NOT NULL,
  updated_at datetime DEFAULT NULL,
  updated_by int(11) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY parcel_coverage_suburb_fk (suburb_id),
  KEY parcel_coverage_branchoffice_fk (branchoffice_id),
  CONSTRAINT parcel_coverage_suburb_fk FOREIGN KEY (suburb_id) REFERENCES suburb (id),
  CONSTRAINT parcel_coverage_branchoffice_fk FOREIGN KEY (branchoffice_id) REFERENCES branchoffice (id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;