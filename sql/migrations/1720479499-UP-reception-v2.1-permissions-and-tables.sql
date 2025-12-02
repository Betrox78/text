-- 1720479499 UP reception v2.1 permissions and tables
CREATE TABLE parcels_init_config (
  id INT NOT NULL AUTO_INCREMENT,
  employee_id INT NOT NULL,
  terminal_origin_id INT DEFAULT NULL,
  enable_terminal_origin_id BOOLEAN DEFAULT TRUE,
  shipment_type ENUM('OCU','EAD','RAD/OCU','RAD/EAD') DEFAULT 'OCU',
  enable_is_rad BOOLEAN DEFAULT TRUE,
  enable_is_ead BOOLEAN DEFAULT TRUE,
  sender_id INT DEFAULT NULL,
  enable_sender_id BOOLEAN DEFAULT TRUE,
  sender_zip_code INT DEFAULT NULL,
  enable_sender_zip_code BOOLEAN DEFAULT TRUE,
  terminal_destiny_id INT DEFAULT NULL,
  enable_terminal_destiny_id BOOLEAN DEFAULT TRUE,
  addressee_id INT DEFAULT NULL,
  enable_addressee_id BOOLEAN DEFAULT TRUE,
  addressee_zip_code INT DEFAULT NULL,
  enable_addressee_zip_code BOOLEAN DEFAULT TRUE,
  pays_sender BOOLEAN DEFAULT TRUE,
  enable_pays_sender BOOLEAN DEFAULT TRUE,
  is_credit BOOLEAN DEFAULT FALSE,
  enable_is_credit BOOLEAN DEFAULT TRUE,
  is_internal_parcel BOOLEAN DEFAULT FALSE,
  enable_is_internal_parcel BOOLEAN DEFAULT TRUE,
  send_whatsapp_notification BOOLEAN DEFAULT FALSE,
  enable_send_whatsapp_notification BOOLEAN DEFAULT TRUE,
  status TINYINT NOT NULL DEFAULT '1',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by INT NOT NULL,
  updated_at DATETIME DEFAULT NULL,
  updated_by INT DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE unique_parcels_init_config_employee_id_idx(employee_id),
  CONSTRAINT parcels_init_config_employee_id_fk FOREIGN KEY (employee_id) REFERENCES employee(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT parcels_init_config_terminal_destiy_id_fk FOREIGN KEY (terminal_destiny_id) REFERENCES branchoffice(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT parcels_init_config_terminal_origin_id_fk FOREIGN KEY (terminal_origin_id) REFERENCES branchoffice(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT parcels_init_config_sender_id_fk FOREIGN KEY (sender_id) REFERENCES customer(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT parcels_init_config_addressee_id_fk FOREIGN KEY (addressee_id) REFERENCES customer(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT parcels_init_config_created_by_fk FOREIGN KEY (created_by) REFERENCES customer(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT parcels_init_updated_by_fk FOREIGN KEY (updated_by) REFERENCES customer(id) ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE parcels_branchoffices_init_config (
  id INT NOT NULL AUTO_INCREMENT,
  employee_id INT NOT NULL,
  branchoffice_id INT DEFAULT NULL,
  sender_id INT DEFAULT NULL,
  sender_zip_code INT DEFAULT NULL,
  addressee_id INT DEFAULT NULL,
  addressee_zip_code INT DEFAULT NULL,
  show_in_origin BOOLEAN NOT NULL DEFAULT TRUE,
  show_in_destiny BOOLEAN NOT NULL DEFAULT TRUE,
  status TINYINT NOT NULL DEFAULT '1',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by INT NOT NULL,
  updated_at DATETIME DEFAULT NULL,
  updated_by INT DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE idx_employee_branchoffice (employee_id, branchoffice_id),
  CONSTRAINT parcels_branchoffices_init_config_employee_id_fk FOREIGN KEY (employee_id) REFERENCES employee(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT parcels_branchoffices_init_config_branchoffice_id_fk FOREIGN KEY (branchoffice_id) REFERENCES branchoffice(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT parcels_branchoffices_init_config_sender_id_fk FOREIGN KEY (sender_id) REFERENCES customer(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT parcels_branchoffices_init_config_addressee_id_fk FOREIGN KEY (addressee_id) REFERENCES customer(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT parcels_branchoffices_init_config_created_by_fk FOREIGN KEY (created_by) REFERENCES customer(id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT parcels_branchoffices_init_config_updated_by_fk FOREIGN KEY (updated_by) REFERENCES customer(id) ON UPDATE NO ACTION ON DELETE NO ACTION
);


INSERT INTO sub_module(id, name, module_id, group_type, menu_type, created_by)
VALUES(149, 'app.parcel.reception_v2-1', 2, 'parcel', 'p_sub_parcel', 1);

INSERT into permission (name, description, sub_module_id,created_by)
VALUES ('#create', 'Documentar paqueteria v2.1', 149, 1);