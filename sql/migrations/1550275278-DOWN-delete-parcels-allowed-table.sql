-- 1550275278 DOWN delete-parcels-allowed-table
CREATE TABLE `parcels_allowed` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
`name` varchar(150) NOT NULL,
`description` varchar(254) NOT NULL,
`has_insurance` tinyint(1) NOT NULL DEFAULT '0',
`insurance_max_amount` decimal(12,2) DEFAULT NULL,
`need_auth` tinyint(1) NOT NULL DEFAULT '0',
`need_documentation` tinyint(1) NOT NULL DEFAULT '0',
`status` tinyint(4) NOT NULL DEFAULT '1',
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`created_by` int(11) NOT NULL,
`updated_at` datetime DEFAULT NULL,
`updated_by` int(11) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

ALTER TABLE parcels_packages
ADD COLUMN parcel_allowed_id int(11) NOT NULL,
CONSTRAINT parcels_packages_parcel_allowed_id_fk FOREIGN KEY (parcel_allowed_id) REFERENCES parcels_allowed(id)
;