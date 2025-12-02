-- 1546628324 UP add-alliance-category
CREATE TABLE alliance_category (
  id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(254) NOT NULL,
  description VARCHAR(512) NOT NULL,
  status TINYINT(4) NULL DEFAULT 1,
  created_by INT NOT NULL,
  created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by INT NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id)
);