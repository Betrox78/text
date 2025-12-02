-- 1545094558 UP add-alliance-city
CREATE TABLE alliance_city (
  id INT NOT NULL AUTO_INCREMENT,
  alliance_id INT NOT NULL,
  city_id INT NOT NULL,
  status TINYINT(4) NOT NULL DEFAULT 1,
  created_by INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by INT NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY fk_alliance_city_alliance_id (alliance_id),
  KEY fk_alliance_city_city_id (city_id),
  CONSTRAINT fk_alliance_city_alliance_id FOREIGN KEY (alliance_id) REFERENCES alliance (id),
  CONSTRAINT fk_alliance_city_city_id FOREIGN KEY (city_id) REFERENCES city (id)
  );