-- 1643904048 UP create-boarding-invoice-complement
CREATE TABLE `boardingpass_invoice_complement` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `id_boardingpass` int(11) NOT NULL,
  `tipo_cfdi` enum('traslado','traslado con complemento cp','ingreso','ingreso con complemento cp') NOT NULL,
  `cfdi_body` longtext,
  `cadena_original` longtext,
  `acuse` varchar(1000) DEFAULT NULL,
  `pdf` varchar(1000) DEFAULT NULL,
  `xml` longtext,
  `system_origin` enum('bitacora','app', 'site') DEFAULT NULL,
  `stamp_date` datetime DEFAULT NULL,
  `stamp_by` int(11) DEFAULT NULL,
  `status_cfdi` enum('documentado','en proceso manifest','en proceso','timbrado','cancelado','Error timbrado') DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_boardingpass_invoice_complement_id` (`id_boardingpass`),
  CONSTRAINT `fk_boardingpass_invoice_complement_id` FOREIGN KEY (`id_boardingpass`) REFERENCES `boarding_pass` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=158 DEFAULT CHARSET=latin1