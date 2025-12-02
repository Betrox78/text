-- 1738296890 UP integration-partner-customer-address
CREATE TABLE `integration_partner_customer_address` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `integration_partner_customer_id` INT NOT NULL,
    `customer_address_id` INT NOT NULL,
    `status` tinyint NOT NULL DEFAULT '1',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by` int NOT NULL,
    `updated_at` datetime DEFAULT NULL,
    `updated_by` int DEFAULT NULL,
    KEY `integration_partner_customer_address_status_idx` (`status`),
    KEY `integration_partner_customer_address_created_at_idx` (`created_at`),
    FOREIGN KEY (`integration_partner_customer_id`) REFERENCES `integration_partner_customer` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    FOREIGN KEY (`customer_address_id`) REFERENCES `customer_addresses`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    UNIQUE KEY `integration_partner_customer_address_uk` (`integration_partner_customer_id`, `customer_address_id`)
);