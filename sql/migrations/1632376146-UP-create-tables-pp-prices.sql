-- 1632376146 UP create-tables-pp-prices
CREATE TABLE IF NOT EXISTS pp_price_km
(
    id INT(11) NOT NULL AUTO_INCREMENT,
    min_km decimal(12,2)    NOT NULL,
    max_km  decimal(12,2)    NOT NULL,
    price  decimal(12,2)    NOT NULL,
    currency_id  int(11)     NOT NULL,
    parent_id int(11),
    shipping_type  ENUM('parcel', 'courier') NULL DEFAULT 'parcel',
   status tinyint(4) NOT NULL DEFAULT '1',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by int(11) DEFAULT NULL,
  updated_at datetime DEFAULT NULL,
  updated_by int(11) DEFAULT NULL,
   PRIMARY KEY (id),
   CONSTRAINT fk_pp_price_km_currency_id FOREIGN KEY (currency_id) REFERENCES currency(id),
   CONSTRAINT fk_pp_price_km_id FOREIGN KEY (parent_id) REFERENCES pp_price_km(id)
);

  CREATE TABLE IF NOT EXISTS pp_price
  (
      id               int(11)    NOT NULL AUTO_INCREMENT,
      name_price varchar(45)    NOT NULL,
      min_linear_volume  decimal(12,2)    NOT NULL,
      max_linear_volume  decimal(12,2)    NOT NULL,
      min_weight  decimal(12,2)    NOT NULL,
      max_weight decimal(12,2)    NOT NULL,
      price  decimal(12,2)    NOT NULL,
      currency_id int(11) NOT NULL,
      parent_id int(11) ,
      shipping_type ENUM('parcel', 'courier') NOT NULL DEFAULT 'parcel',
      status tinyint(4) NOT NULL DEFAULT '1',
     created_at datetime NULL DEFAULT CURRENT_TIMESTAMP,
     created_by int(11) NOT NULL,
     updated_at datetime DEFAULT NULL,
     updated_by int(11) DEFAULT NULL,
     PRIMARY KEY (id),
     CONSTRAINT fk_pp_price_currency_id FOREIGN KEY (currency_id) REFERENCES currency(id),
     CONSTRAINT fk_pp_price_id FOREIGN KEY (parent_id) REFERENCES pp_price(id)
  ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8;