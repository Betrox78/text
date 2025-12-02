-- 1547154333 UP create-products-table
CREATE TABLE products (
  id int(11) NOT NULL AUTO_INCREMENT,
  sku varchar(50) NOT NULL COMMENT 'Código único que el cliente puede definir',
  name varchar(50) NOT NULL DEFAULT '',
  description text,
  cost decimal(12,2) NOT NULL DEFAULT 0.00,
  expires tinyint(1) NOT NULL DEFAULT 1 COMMENT '¿Caduca?',
  has_stock tinyint(1) NOT NULL DEFAULT 1 COMMENT '¿Lleva inventario?',
  status tinyint(1) NOT NULL DEFAULT 1,
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by int(11) DEFAULT NULL,
  updated_at datetime DEFAULT NULL,
  updated_by int(11) DEFAULT NULL,
  PRIMARY KEY (id),
  INDEX (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;