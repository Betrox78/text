-- 1698187042 UP integration-partner-customer

ALTER TABLE `integration_partner`

ADD COLUMN `user_id` INT DEFAULT NULL AFTER `name`,
ADD CONSTRAINT `integration_partner_user_id_fk` FOREIGN KEY (user_id)
    REFERENCES customer (id) ON DELETE RESTRICT ON UPDATE RESTRICT,

ADD COLUMN `branchoffice_id` INT DEFAULT NULL AFTER `user_id`,
ADD CONSTRAINT `integration_partner_branchoffice_id_fk` FOREIGN KEY (branchoffice_id)
    REFERENCES branchoffice (id) ON DELETE RESTRICT ON UPDATE RESTRICT,

ADD COLUMN `customer_id` INT DEFAULT NULL AFTER `branchoffice_id`,
ADD CONSTRAINT `integration_partner_customer_id_fk` FOREIGN KEY (customer_id)
    REFERENCES customer (id) ON DELETE RESTRICT ON UPDATE RESTRICT,

ADD COLUMN `customer_addresses_id` INT DEFAULT NULL AFTER `customer_id`,
ADD CONSTRAINT `integration_partner_customer_addresses_id_fk` FOREIGN KEY (customer_addresses_id)
    REFERENCES customer_addresses (id) ON DELETE RESTRICT ON UPDATE RESTRICT,

ADD COLUMN `customer_billing_information_id` INT DEFAULT NULL AFTER `customer_addresses_id`,
ADD CONSTRAINT `integration_partner_customer_billing_information_id_fk` FOREIGN KEY (customer_billing_information_id)
    REFERENCES customer_billing_information (id) ON DELETE RESTRICT ON UPDATE RESTRICT;
