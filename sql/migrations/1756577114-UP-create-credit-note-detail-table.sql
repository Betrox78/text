-- 1756577114 UP create-credit-note-detail-table
CREATE TABLE `credit_note_detail` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `credit_note_id` INT DEFAULT NULL,
    `invoice_id` INT DEFAULT NULL,
    `amount` DECIMAL(12 , 2 ) NOT NULL DEFAULT '0.00',
    `iva_amount` DECIMAL(12 , 2 ) NOT NULL DEFAULT '0.00',
    `iva_withheld_amount` DECIMAL(12 , 2 ) NOT NULL DEFAULT '0.00',
    `status` TINYINT DEFAULT '0',
    `created_by` INT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_by` INT DEFAULT NULL,
    `updated_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `credit_note_detail_credit_note_id_idx` (`credit_note_id`),
    KEY `credit_note_detail_invoice_id_idx` (`invoice_id`),
    KEY `credit_note_detail_status_idx` (`status`),
    KEY `credit_note_detail_created_at_idx` (`created_at`),
    CONSTRAINT `credit_note_detail_credit_note_id_fk` FOREIGN KEY (`credit_note_id`)
        REFERENCES `credit_note` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `credit_note_detail_invoice_id_fk` FOREIGN KEY (`invoice_id`)
        REFERENCES `invoice` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `credit_note_detail_created_by_fk` FOREIGN KEY (`created_by`)
        REFERENCES `users` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `credit_note_detail_updated_by_fk` FOREIGN KEY (`updated_by`)
        REFERENCES `users` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
);
