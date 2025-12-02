-- 1680043596 DOWN modify-columns

ALTER TABLE prepaid_package_travel
DROP FOREIGN KEY `fk_prepaid_package_travel_branchoffice_id`;
ALTER TABLE prepaid_package_travel
DROP FOREIGN KEY `fk_prepaid_package_travel_prepaid_config_id`;

ALTER TABLE prepaid_package_travel
DROP COLUMN branchoffice_id,
DROP COLUMN prepaid_status,
DROP COLUMN active_tickets,
DROP COLUMN used_tickets,
DROP COLUMN total_tickets,
DROP COLUMN payment_condition,
DROP COLUMN debt,
DROP COLUMN iva,
DROP COLUMN purchase_origin,
DROP COLUMN prepaid_package_config_id;

-- /////////
ALTER TABLE tickets
DROP FOREIGN KEY tickets_prepaid_package_travel_id_fk;

ALTER TABLE tickets
DROP COLUMN prepaid_travel_id;

-- ///////////
ALTER TABLE payment
DROP FOREIGN KEY fk_payment_prepaid_travel_id;

ALTER TABLE payment
DROP COLUMN prepaid_travel_id;