-- 1682192723 UP integration-partner-session

CREATE TABLE integration_partner_session (
  id int(11) NOT NULL AUTO_INCREMENT,
  token varchar(512) NOT NULL,
  integration_partner_id int(11) DEFAULT NULL,
  status tinyint(4) DEFAULT '1',
  created_by int(11) NOT NULL,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  updated_by int(11) DEFAULT NULL,
  updated_at datetime DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_ips_integration_partner_id (integration_partner_id),
  KEY idx_ips_token_integration_partner_id_status (token, integration_partner_id, status),
  CONSTRAINT fk_ips_integration_partner_id FOREIGN KEY (integration_partner_id) REFERENCES integration_partner (id)
)

