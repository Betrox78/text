-- 1698187042 DOWN integration-partner-customer

ALTER TABLE `integration_partner` DROP FOREIGN KEY `integration_partner_user_id_fk`;
ALTER TABLE `integration_partner` DROP COLUMN `user_id`;


ALTER TABLE `integration_partner` DROP FOREIGN KEY `integration_partner_branchoffice_id_fk`;
ALTER TABLE `integration_partner` DROP COLUMN `branchoffice_id`;

ALTER TABLE `integration_partner` DROP FOREIGN KEY `integration_partner_customer_id_fk`;
ALTER TABLE `integration_partner` DROP COLUMN `customer_id`;

ALTER TABLE `integration_partner` DROP FOREIGN KEY `integration_partner_customer_addresses_id_fk`;
ALTER TABLE `integration_partner` DROP COLUMN `customer_addresses_id`;

ALTER TABLE `integration_partner` DROP FOREIGN KEY `integration_partner_customer_billing_information_id_fk`;
ALTER TABLE `integration_partner` DROP COLUMN `customer_billing_information_id`;
