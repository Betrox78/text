-- 1543953480 UP add-income
CREATE TABLE income (
  id INT(11) NOT NULL AUTO_INCREMENT,
  type_income ENUM('public', 'invoice') NOT NULL DEFAULT 'public',
  reference VARCHAR(254) NOT NULL,
  exchange_rate_id INT(11) NOT NULL,
  currency_id INT(11) NOT NULL,
  status TINYINT(4) NOT NULL DEFAULT '1',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by INT(11) DEFAULT NULL,
  updated_at DATETIME DEFAULT NULL,
  updated_by INT(11) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY fk_income_currency_id (currency_id),
  KEY fk_income_exchange_rate_id (exchange_rate_id),
  CONSTRAINT fk_income_currency_id FOREIGN KEY (currency_id) REFERENCES currency (id),
  CONSTRAINT fk_income_exchange_rate_id FOREIGN KEY (exchange_rate_id) REFERENCES exchange_rate (id)
);

