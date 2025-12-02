-- 1758260730 UP create-credit-note-services-table
CREATE TABLE `credit_note_services` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `credit_note_id` INT DEFAULT NULL,
    `parcel_id` INT DEFAULT NULL,
    `parcel_prepaid_id` INT DEFAULT NULL,
    `prev_debt` DECIMAL(12, 2) NOT NULL DEFAULT '0.00',
    `new_debt` DECIMAL(12, 2) NOT NULL DEFAULT '0.00',
    `created_by` INT NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_by` INT DEFAULT NULL,
    `updated_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `credit_note_services_credit_note_id_idx` (`credit_note_id`),
    KEY `credit_note_services_parcel_id_idx` (`parcel_id`),
    KEY `credit_note_services_parcel_prepaid_id_idx` (`parcel_prepaid_id`),
    KEY `credit_note_services_created_at_idx` (`created_at`),
    CONSTRAINT `credit_note_services_credit_note_id_fk` FOREIGN KEY (`credit_note_id`)
        REFERENCES `credit_note` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `credit_note_services_parcel_id_fk` FOREIGN KEY (`parcel_id`)
        REFERENCES `parcels` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `credit_note_services_parcel_prepaid_id_fk` FOREIGN KEY (`parcel_prepaid_id`)
        REFERENCES `parcels_prepaid` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `credit_note_services_created_by_fk` FOREIGN KEY (`created_by`)
        REFERENCES `users` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `credit_note_services_updated_by_fk` FOREIGN KEY (`updated_by`)
        REFERENCES `users` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
);
