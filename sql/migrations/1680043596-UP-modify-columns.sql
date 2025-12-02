-- 1680043596 UP modify-columns

-- /////////
ALTER TABLE prepaid_package_travel
ADD COLUMN branchoffice_id int(11) DEFAULT NULL,
ADD COLUMN prepaid_status int(11) DEFAULT 0,
ADD COLUMN active_tickets int(11) DEFAULT 0,
ADD COLUMN used_tickets int(11) DEFAULT 0,
ADD COLUMN total_tickets int(11) default 0,
ADD COLUMN payment_condition ENUM('credit', 'cash') NOT NULL DEFAULT 'cash',
ADD COLUMN debt FLOAT(12,2) DEFAULT 0,
ADD COLUMN iva decimal(12,2)  NOT NULL DEFAULT 0.00,
ADD COLUMN purchase_origin ENUM('sucursal','web','kiosko','app cliente','app chofer') NOT NULL default 'sucursal',
ADD COLUMN prepaid_package_config_id int(11) NOT NULL;

ALTER TABLE `prepaid_package_travel`
ADD CONSTRAINT `fk_prepaid_package_travel_branchoffice_id`
FOREIGN KEY (`branchoffice_id`)
REFERENCES `branchoffice` (`id`);

-- CREATE INDEX branchoffice_id_idx ON prepaid_package_travel(branchoffice_id);

ALTER TABLE `prepaid_package_travel`
ADD CONSTRAINT `fk_prepaid_package_travel_prepaid_config_id`
FOREIGN KEY (`prepaid_package_config_id`)
REFERENCES `prepaid_package_config` (`id`);

-- CREATE INDEX prepaid_package_config_id_idx ON prepaid_package_travel(prepaid_package_config_id);

-- /////////
ALTER TABLE tickets
ADD COLUMN prepaid_travel_id int(11) DEFAULT NULL ;

ALTER TABLE tickets
ADD CONSTRAINT tickets_prepaid_package_travel_id_fk
FOREIGN KEY (prepaid_travel_id)
REFERENCES prepaid_package_travel (id);

ALTER TABLE payment
ADD COLUMN prepaid_travel_id int(11) DEFAULT NULL;

ALTER TABLE payment ADD CONSTRAINT fk_payment_prepaid_travel_id FOREIGN KEY (prepaid_travel_id)
REFERENCES prepaid_package_travel (id);
-- ////////////
