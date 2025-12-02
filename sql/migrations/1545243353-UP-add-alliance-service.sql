-- 1545243353 UP add-alliance-service
CREATE TABLE alliance_service (
  id INT NOT NULL AUTO_INCREMENT,
  alliance_id INT NOT NULL,
  name VARCHAR(254) NOT NULL,
  description VARCHAR(254) NOT NULL,
  unit_price DECIMAL(12,2) NULL DEFAULT 0.0,
  status TINYINT(4) NOT NULL DEFAULT 1,
  created_by INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by INT NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY fk_alliance_service_alliance_id (alliance_id),
  CONSTRAINT fk_alliance_service_alliance_id FOREIGN KEY (alliance_id) REFERENCES alliance (id)
  );