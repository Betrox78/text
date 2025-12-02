-- 1738296693 UP integration-partner-customer
CREATE TABLE `integration_partner_customer` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `customer_code` varchar(64) DEFAULT NULL,
  `integration_partner_id` int DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `phone` varchar(13) NOT NULL,
  `email` varchar(100) NOT NULL DEFAULT '',
  `status` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int DEFAULT NULL,
  UNIQUE KEY `integration_partner_customer_customer_code_uk` (`customer_code`),
  KEY `integration_partner_customer_integration_partner_id_fk` (`integration_partner_id`),
  KEY `integration_partner_customer_status_idx` (`status`),
  KEY `integration_partner_customer_created_at_idx` (`created_at`),
  CONSTRAINT `integration_partner_customer_integration_partner_id_fk` FOREIGN KEY (`integration_partner_id`) REFERENCES `integration_partner` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
);