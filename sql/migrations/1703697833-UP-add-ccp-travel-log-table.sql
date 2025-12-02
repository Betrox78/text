-- 1703697833 UP add-ccp-travel-log-table
CREATE TABLE `travel_logs_ccp` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `travel_log_id` INT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT '1',
  `invoice_status` ENUM('documentado', 'en proceso', 'timbrado', 'cancelado', 'Error timbrado') NOT NULL,
  `xml` longtext,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NULL DEFAULT NULL,
  `created_by` int NOT NULL,
  `updated_by` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_travel_log_ccp_id` (`travel_log_id`),
  CONSTRAINT fk_travel_log_ccp_travel_log_id FOREIGN KEY (travel_log_id) REFERENCES travel_logs(id)
);