-- 1756577129 UP create-credit-note-invoice-table
CREATE TABLE `credit_note_invoice` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `credit_note_id` INT NOT NULL,
  `xml` LONGTEXT,
  `status` ENUM('pending', 'done', 'error', 'cancelled') NOT NULL DEFAULT 'pending',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `parcel_invoice_complement_credit_note_id_idx` (`credit_note_id`),
  KEY `parcel_invoice_complement_status_idx` (`status`),
  KEY `parcel_invoice_complement_created_at_idx` (`created_at`),
  CONSTRAINT `parcel_invoice_complement_credit_note_id_fk`
    FOREIGN KEY (`credit_note_id`) REFERENCES `credit_note` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT
);