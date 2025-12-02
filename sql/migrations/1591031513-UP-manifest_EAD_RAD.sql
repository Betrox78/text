CREATE TABLE  IF NOT EXISTS `vehicle_rad_ead` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
   `id_branchoffice` int(10) NOT NULL,   
   `id_vehicle` int(10) NOT NULL,   
    `id_employee` int(10) NOT NULL,
	   `status` tinyint(4) NOT NULL DEFAULT '2',
	   `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	   `created_by` int(11) DEFAULT NULL,
	   `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	   `updated_by` int(11) DEFAULT NULL,
		PRIMARY KEY (`id`),
	  	FOREIGN KEY (`id_branchoffice`) REFERENCES `branchoffice` (`id`),
	  	FOREIGN KEY (`id_vehicle`) REFERENCES `vehicle` (`id`),
	  	FOREIGN KEY (`id_employee`) REFERENCES `employee` (`id`)
 );



 CREATE TABLE IF NOT EXISTS `parcels_manifest` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
   	`folio` varchar(100) UNIQUE ,
   `id_type_service` int(10) NOT NULL ,
   `id_vehicle_rad_ead` int(10) NOT NULL,  
    `id_branchoffice` int(10) NOT NULL,
	`printing_date` datetime ,
	`printing_date_updated_at` datetime  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `print_number` int(10) NOT NULL DEFAULT 0,   
	   `status` tinyint(4) NOT NULL DEFAULT '2',
	   `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	   `created_by` int(11) DEFAULT NULL,
	   `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	   `updated_by` int(11) DEFAULT NULL,
   PRIMARY KEY (`Id`),
	  	FOREIGN KEY (`id_branchoffice`) REFERENCES `branchoffice` (`id`),
        FOREIGN KEY (`id_vehicle_rad_ead`) REFERENCES `vehicle_rad_ead` (`id`),
          FOREIGN KEY (`id_type_service`) REFERENCES `parcel_service_type` (`id`)
   
 );
  CREATE TABLE IF NOT EXISTS parcels_rad_ead(
  id int(11) NOT NULL AUTO_INCREMENT,
  amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  status tinyint(4) NOT NULL DEFAULT 1,
  parcel_id int(11) NOT NULL,
  count_number_not_delivery int(2)  DEFAULT 0,
  id_type_service int(10) NOT NULL ,  
  zip_code int(11) NOT NULL,
  created_at datetime NULL DEFAULT CURRENT_TIMESTAMP,
  created_by int(11) NOT NULL,
 updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by int(11) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY parcels_id_fk (parcel_id),
  CONSTRAINT parcels_id_fk FOREIGN KEY (parcel_id) REFERENCES parcels (id),
 CONSTRAINT parcel_service_type_id_fk2 FOREIGN KEY (id_type_service) REFERENCES parcel_service_type (id)
  ) ;
  CREATE TABLE IF NOT EXISTS `parcels_manifest_detail` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
   `id_parcels_manifest` int(10) NOT NULL ,
   `id_parcels_rad_ead` int(10) NOT NULL ,  
    `id_reason_no_rad_ead` int(10) ,   
	`other_reasons_not_rad_ead` varchar(100)  ,
     `confirmation_time_ead_rad` datetime  ,
    `confirmation_ead_rad` tinyint(2)  DEFAULT '0',
	   `status` tinyint(5) NOT NULL DEFAULT '2',
	   `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	   `created_by` int(11) DEFAULT NULL,
	   `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	   `updated_by` int(11) DEFAULT NULL,
   PRIMARY KEY (`id`),
     FOREIGN KEY (`id_reason_no_rad_ead`) REFERENCES `delivery_attempt_reason` (`id`),
     FOREIGN KEY (`id_parcels_manifest`) REFERENCES `parcels_manifest` (`id`),
     FOREIGN KEY (`id_parcels_rad_ead`) REFERENCES `parcels_rad_ead` (`id`)
 );

 
 