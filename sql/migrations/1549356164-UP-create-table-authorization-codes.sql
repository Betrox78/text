-- 1549356164 UP create-table-authorization-codes
CREATE TABLE IF NOT EXISTS authorization_codes(
  id INT NOT NULL AUTO_INCREMENT,
  code VARCHAR(50) NOT NULL UNIQUE,
  vigency DATETIME NOT NULL,
  schedule_route_destination_id INT(11) NOT NULL,
  type ENUM('boarding', 'parcel') NOT NULL DEFAULT 'boarding',
  code_status ENUM('active', 'used', 'expired') NOT NULL DEFAULT 'active',
  authorizated_by INT(11) NOT NULL,
  created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  status INT(11) NOT NULL DEFAULT 1,
  created_by INT NULL,
  updated_at DATETIME NULL,
  updated_by INT NULL,
  CONSTRAINT fk_auth_codes_schedule_route_destination_id FOREIGN KEY (schedule_route_destination_id) REFERENCES schedule_route_destination(id),
  CONSTRAINT fk_auth_codes_authorizated_by FOREIGN KEY (authorizated_by) REFERENCES employee(id),
  PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8;