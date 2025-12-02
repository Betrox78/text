-- 1743828498 UP create-paym-comp-detail-table
CREATE TABLE `payment_complement_detail` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `payment_complement_id` INT DEFAULT NULL,
    `invoice_id` INT DEFAULT NULL,
    `invoice_uuid` VARCHAR(50) DEFAULT NULL,
    `invoice_folio` VARCHAR(50) DEFAULT NULL,
    `installment_number` INT NOT NULL,
    `previous_debt_amount` DECIMAL(12 , 2 ) NOT NULL DEFAULT '0.00',
    `paid_amount` DECIMAL(12 , 2 ) NOT NULL DEFAULT '0.00',
    `new_debt_amount` DECIMAL(12 , 2 ) NOT NULL DEFAULT '0.00',
    `iva_amount` DECIMAL(12 , 2 ) NOT NULL DEFAULT '0.00',
    `iva_withheld_amount` DECIMAL(12 , 2 ) NOT NULL DEFAULT '0.00',
    `status` TINYINT DEFAULT '0',
    `created_by` INT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_by` INT DEFAULT NULL,
    `updated_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `payment_complement_detail_payment_complement_id_idx` (`payment_complement_id`),
    KEY `payment_complement_detail_invoice_id_idx` (`invoice_id`),
    KEY `payment_complement_detail_status_idx` (`status`),
    KEY `payment_complement_detail_created_at_idx` (`created_at`),
    CONSTRAINT `payment_complement_detail_payment_complement_id_fk` FOREIGN KEY (`payment_complement_id`)
        REFERENCES `payment_complement` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `payment_complement_detail_invoice_id_fk` FOREIGN KEY (`invoice_id`)
        REFERENCES `invoice` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `payment_complement_detail_created_by_fk` FOREIGN KEY (`created_by`)
        REFERENCES `users` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `payment_complement_detail_updated_by_fk` FOREIGN KEY (`updated_by`)
        REFERENCES `users` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
);