-- 1570120516 UP create-table-prices-list
CREATE TABLE IF NOT EXISTS prices_lists(
  id int(11) NOT NULL AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  description text DEFAULT NULL,
  is_default tinyint(1) DEFAULT 0,
  created_at datetime NULL DEFAULT CURRENT_TIMESTAMP,
  created_by int(11) NOT NULL,
  updated_at datetime DEFAULT NULL,
  updated_by int(11) DEFAULT NULL,
  PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8;