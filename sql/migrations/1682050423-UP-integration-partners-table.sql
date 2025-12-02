-- 1682050423 UP integration-partners-table

CREATE TABLE integration_partner (
  id int(11) NOT NULL AUTO_INCREMENT,
  name varchar(254) NOT NULL,
  status tinyint(4) DEFAULT '1',
  created_by int(11) NOT NULL,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  updated_by int(11) DEFAULT NULL,
  updated_at datetime DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_partner_id_status (id, status)
)