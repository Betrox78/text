-- 1543954693 UP add-income-movement

CREATE TABLE income_movement (
  id INT(11) NOT NULL AUTO_INCREMENT,
  income_id INT(11) NOT NULL,
  ticket_id INT(11) NOT NULL,
  nature ENUM('debt', 'credit') NOT NULL DEFAULT 'debt',
  amount DECIMAL(12,2) NOT NULL,
  reference VARCHAR(254) NOT NULL,
  description VARCHAR(254) NOT NULL,
  exchange_rate_id INT(11) NOT NULL,
  currency_id INT(11) NOT NULL,
  status TINYINT(4) NOT NULL DEFAULT '1',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by INT(11) DEFAULT NULL,
  updated_at DATETIME DEFAULT NULL,
  updated_by INT(11) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY fk_income_movement_income_id (income_id),
  KEY fk_income_movement_ticket_id (ticket_id),
  KEY fk_income_movement_expense_currency_id (currency_id),
  KEY fk_expense_exchange_rate_id (exchange_rate_id),
  CONSTRAINT fk_income_movement_income_id FOREIGN KEY (income_id) REFERENCES income (id),
  CONSTRAINT fk_income_movement_ticket_id FOREIGN KEY (ticket_id) REFERENCES tickets (id),
  CONSTRAINT fk_income_movement_currency_id FOREIGN KEY (currency_id) REFERENCES currency (id),
  CONSTRAINT fk_income_movement_exchange_rate_id FOREIGN KEY (exchange_rate_id) REFERENCES exchange_rate (id)
);
