-- 1570123780 UP create-table-prices_lists_details
CREATE TABLE IF NOT EXISTS prices_lists_details(
  id int(11) NOT NULL AUTO_INCREMENT,
  price_list_id int(11) NOT NULL,
  terminal_origin_id int(11) NOT NULL,
  terminal_destiny_id int(11) NOT NULL,
  stops int(5),
  special_ticket_id int(11) NOT NULL,
  base tinyint(1),
  discount decimal,
  amount decimal,
  total_amount decimal,
  created_at datetime NULL DEFAULT CURRENT_TIMESTAMP,
  created_by int(11) NOT NULL,
  updated_at datetime DEFAULT NULL,
  updated_by int(11) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_special_ticket_price (special_ticket_id,price_list_id, terminal_origin_id, terminal_destiny_id, stops),
  CONSTRAINT fk_prices_list_id FOREIGN KEY (price_list_id) REFERENCES prices_lists(id) ON DELETE CASCADE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;