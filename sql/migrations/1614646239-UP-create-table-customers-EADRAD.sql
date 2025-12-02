-- 1614646239 UP create-table-customers-EADRAD
CREATE TABLE IF NOT EXISTS customer_rad_ead (
`id` int(11) NOT NULL AUTO_INCREMENT,
 `customer_id` int(11) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
   `created_by` int(11) DEFAULT NULL,
   `service_cost` decimal(12,2) DEFAULT 0 COMMENT 'Importe que se le cobrara al cliente',
   `excess_cost` decimal(12,2) DEFAULT 0 COMMENT 'Importe de excedente',
   `status` int(11) NOT NULL DEFAULT '1',
   `updated_at` datetime DEFAULT NULL,
   `updated_by` int(11) DEFAULT NULL,
   PRIMARY KEY (`id`),
   KEY `customer_rad_ead_customer_id_idx` (`customer_id`),
   CONSTRAINT `customer_rad_ead_customer_id` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=latin1;