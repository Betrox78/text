-- 1743852193 UP create-paym-invoice-table
CREATE TABLE `payment_complement_invoice` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `payment_complement_id` INT NOT NULL,
  `xml` LONGTEXT,
  `status` ENUM('pending', 'done', 'error', 'cancelled') NOT NULL DEFAULT 'pending',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `parcel_invoice_complement_payment_complement_id_idx` (`payment_complement_id`),
  KEY `parcel_invoice_complement_status_idx` (`status`),
  KEY `parcel_invoice_complement_created_at_idx` (`created_at`),
  CONSTRAINT `parcel_invoice_complement_payment_complement_id_fk`
    FOREIGN KEY (`payment_complement_id`) REFERENCES `payment_complement` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT
);