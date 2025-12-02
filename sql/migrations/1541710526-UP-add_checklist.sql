-- 1541710526 UP add_checklist
CREATE TABLE  checklist_vans (
  id int(11) NOT NULL AUTO_INCREMENT,
  name varchar(100) NOT NULL,
  category enum('addon','tool','documentation') NOT NULL,
  is_default tinyint(1) DEFAULT '0',
  default_value int(11) DEFAULT NULL,
  use_price decimal(12,2) NOT NULL DEFAULT '0.00',
  damage_price decimal(12,2) NOT NULL DEFAULT '0.00',
  status tinyint(4) NOT NULL DEFAULT '1',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by int(11) DEFAULT NULL,
  updated_at datetime DEFAULT NULL,
  updated_by int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
);