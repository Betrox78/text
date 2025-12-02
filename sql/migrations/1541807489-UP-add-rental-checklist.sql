-- 1541807489 UP add-rental-checklist

CREATE TABLE rental_checklist (
  id int(11) NOT NULL AUTO_INCREMENT,
  rental_id int(11) NOT NULL,
  checklist_van_id int(11) NOT NULL,
  delivery_quantity int(11) NOT NULL DEFAULT 0,
  unit_price decimal(12,2) NOT NULL DEFAULT '0.00',
  amount decimal(12,2) NOT NULL DEFAULT '0.00',
  delivery_notes text DEFAULT NULL,
  reception_quantity int(11) NOT NULL DEFAULT 0,
  damage_quantity int(11) NOT NULL DEFAULT 0,
  damage_percent decimal(12,2) NOT NULL DEFAULT '0.00',
  damage_amount decimal(12,2) NOT NULL DEFAULT '0.00',
  reception_notes text DEFAULT NULL,
  status_on_reception tinyint(4) NOT NULL DEFAULT '0',
  status tinyint(4) NOT NULL DEFAULT '1',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by int(11) NOT NULL,
  updated_at datetime DEFAULT NULL,
  updated_by int(11) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY rental_checklist_rental_fk (rental_id),
  KEY rental_checklist_checklist_van_fk (checklist_van_id),
  CONSTRAINT rental_checklist_rental_fk FOREIGN KEY (rental_id) REFERENCES rental (id),
  CONSTRAINT rental_checklist_checklist_van_fk FOREIGN KEY (checklist_van_id) REFERENCES checklist_vans (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;