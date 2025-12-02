-- 1560325963 UP create table customer promos
CREATE TABLE IF NOT EXISTS customers_promos(
  promo_id int(11) NOT NULL,
  customer_id int(11) NOT NULL,
  usage_limit int(11) NOT NULL DEFAULT 0,
  used int(11) NOT NULL DEFAULT 0,
  status int(11) NOT NULL DEFAULT 1,
  created_at datetime NULL DEFAULT CURRENT_TIMESTAMP,
  created_by int(11) NOT NULL,
  updated_at datetime DEFAULT NULL,
  updated_by int(11) DEFAULT NULL,
  CONSTRAINT fk_customer_promos_promo_id FOREIGN KEY (promo_id) REFERENCES promos(id) ON DELETE CASCADE,
  CONSTRAINT fk_customer_promos_customer_id FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE CASCADE,
  UNIQUE compuest_promo_id_customer_id_idx(promo_id, customer_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8;