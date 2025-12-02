-- 1690776992 UP add-prepaid-travel-invoice-table
CREATE TABLE IF NOT EXISTS `prepaid_travel_invoice_complement` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `id_prepaid` int(11) NOT NULL,
  `cfdi_body` longtext,
  `cadena_original` longtext,
  `acuse` varchar(1000) DEFAULT NULL,
  `pdf` varchar(1000) DEFAULT NULL,
  `xml` longtext,
  `system_origin` enum('bitacora','app','site','admin') DEFAULT NULL,
  `stamp_date` datetime DEFAULT NULL,
  `stamp_by` int(11) DEFAULT NULL,
  `status_cfdi` enum('documentado','en proceso manifest','en proceso','timbrado','cancelado','Error timbrado') DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `tipo_cfdi` enum('ingreso','ingreso con complemento carta porte','factura global') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_prepaid_travel_invoice_complement_id` (`id_prepaid`),
  CONSTRAINT `fk_prepaid_travel_invoice_complement_id` FOREIGN KEY (`id_prepaid`) REFERENCES `prepaid_package_travel` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1