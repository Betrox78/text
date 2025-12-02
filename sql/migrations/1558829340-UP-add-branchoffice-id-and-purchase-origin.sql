-- 1558829340 UP add-branchoffice-id-and-purchase-origin
ALTER TABLE invoice
ADD COLUMN purchase_origin ENUM('sucursal','web','kiosko','app cliente','app chofer') NOT NULL default 'sucursal',
ADD COLUMN branchoffice_id INT(11) NULL,
ADD INDEX invoice_branchoffice_id_idx (branchoffice_id ASC),
ADD CONSTRAINT invoice_branchoffice_id
  FOREIGN KEY (branchoffice_id)
  REFERENCES branchoffice (id)
  ON DELETE RESTRICT
  ON UPDATE RESTRICT;