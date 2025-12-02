-- 1705099763 UP create-ead-rad-ccp-table
CREATE TABLE `parcels_manifest_ccp` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `parcels_manifest_id` INT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT '1',
  `invoice_status` ENUM('documentado', 'en proceso', 'timbrado', 'cancelado', 'Error timbrado') NOT NULL,
  `xml` longtext,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NULL DEFAULT NULL,
  `created_by` int NOT NULL,
  `updated_by` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_parcels_manifest_id` (`parcels_manifest_id`),
  CONSTRAINT fk_parcels_manifest_ccp_parcels_manifest_id FOREIGN KEY (parcels_manifest_id) REFERENCES parcels_manifest(id)
);