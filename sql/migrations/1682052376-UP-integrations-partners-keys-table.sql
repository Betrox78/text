-- 1682052376 UP integrations-partners-keys-table

CREATE TABLE integration_partner_api_key (
  id int(11) NOT NULL AUTO_INCREMENT,
  api_key varchar(254) NOT NULL,
  key_secret varchar(512) NOT NULL,
  integration_partner_id int(11) DEFAULT NULL,
  status tinyint(4) DEFAULT '1',
  created_by int(11) NOT NULL,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  updated_by int(11) DEFAULT NULL,
  updated_at datetime DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_integration_partner_id (integration_partner_id),
  KEY idx_api_key_key_secret_status (api_key, key_secret, status),
  CONSTRAINT fk_integration_partner_id FOREIGN KEY (integration_partner_id) REFERENCES integration_partner (id)
)