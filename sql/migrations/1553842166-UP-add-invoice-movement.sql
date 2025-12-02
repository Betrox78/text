-- 1553842166 UP add-invoice-movement

CREATE TABLE invoice_movement (
  id INT NOT NULL AUTO_INCREMENT,
  invoice_id INT NOT NULL,
  consecutive INT NOT NULL,
  service_code INT NOT NULL,
  warehouse_id INT DEFAULT NULL,
  reference VARCHAR(100) NOT NULL,
  classification_code VARCHAR(100) NOT NULL,
  cost DECIMAL(12,2) NOT NULL DEFAULT '0.00',
  units INT NOT NULL,
  price DECIMAL(12,2) NOT NULL DEFAULT '0.00',
  amount DECIMAL(12,2) NOT NULL DEFAULT '0.00',
  status TINYINT(4) NULL DEFAULT 1,
  created_by INT NOT NULL,
  created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by INT NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY `invoice_movement_invoice_id_fk_key` (`invoice_id`),
  CONSTRAINT `invoice_movement_invoice_id_fk` FOREIGN KEY (`invoice_id`) REFERENCES `invoice` (`id`)
);