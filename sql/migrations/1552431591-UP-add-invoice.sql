-- 1552431591 UP add-invoice
CREATE TABLE invoice (
  id INT NOT NULL AUTO_INCREMENT,
  description VARCHAR(512) NOT NULL,
  amount DECIMAL(12,2) NOT NULL DEFAULT '0.00',
  discount DECIMAL(12,2) NOT NULL DEFAULT '0.00',
  total_amount DECIMAL(12,2) NOT NULL DEFAULT '0.00',
  iva DECIMAL(12,2) NOT NULL DEFAULT '0.00',
  iva_withheld DECIMAL(12,2) NOT NULL DEFAULT '0.00',
  status TINYINT(4) NULL DEFAULT 1,
  invoice_status enum('pending','progress','done','error','expired') NOT NULL,
  created_by INT NOT NULL,
  created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by INT NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id)
);

ALTER TABLE boarding_pass
ADD COLUMN invoice_id INT NULL,
ADD KEY boarding_pass_invoice_id_fk_key (invoice_id),
ADD CONSTRAINT boarding_pass_invoice_id_fk FOREIGN KEY (invoice_id) REFERENCES invoice (id) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE rental
ADD COLUMN invoice_id INT NULL,
ADD KEY rental_invoice_id_fk_key (invoice_id),
ADD CONSTRAINT rental_invoice_id_fk FOREIGN KEY (invoice_id) REFERENCES invoice (id) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE parcels
ADD COLUMN invoice_id INT NULL,
ADD KEY parcels_invoice_id_fk_key (invoice_id),
ADD CONSTRAINT parcels_invoice_id_fk FOREIGN KEY (invoice_id) REFERENCES invoice (id) ON DELETE RESTRICT ON UPDATE RESTRICT;