-- 1589810871 UP create-parcels-rad
CREATE TABLE IF NOT EXISTS `parcel_service_type` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
	`type_service` varchar(10) UNIQUE NOT NULL ,
     `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
     `status` tinyint(4) NOT NULL DEFAULT '2',
   `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
   `created_by` int(11) DEFAULT NULL,
   `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   `updated_by` int(11) DEFAULT NULL,
   PRIMARY KEY (`Id`)
 );

CREATE TABLE IF NOT EXISTS parcels_rad_ead(
  id int(11) NOT NULL AUTO_INCREMENT,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  status tinyint(4) NOT NULL DEFAULT 1,
  parcel_id int(11) NOT NULL,
  id_type_service int(10) NOT NULL ,
  zip_code int(11) NOT NULL,
  created_at datetime NULL DEFAULT CURRENT_TIMESTAMP,
  created_by int(11) NOT NULL,
  updated_at datetime DEFAULT NULL,
  updated_by int(11) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY parcels_id_fk (parcel_id),
  CONSTRAINT parcels_id_fk FOREIGN KEY (parcel_id) REFERENCES parcels (id),
 CONSTRAINT parcel_service_type_id_fk2 FOREIGN KEY (id_type_service) REFERENCES parcel_service_type (id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;