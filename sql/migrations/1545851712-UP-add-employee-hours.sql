-- 1545851712 UP add-employee-hours
CREATE TABLE employee_hour (
  id INT NOT NULL AUTO_INCREMENT,
  employee_id INT NOT NULL,
  branchoffice_id INT NOT NULL,
  register_at DATETIME NOT NULL,
  status TINYINT(4) NOT NULL DEFAULT 1,
  created_by INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by INT NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY fk_employee_hour_employee_id (employee_id),
  CONSTRAINT fk_employee_hour_employee_id FOREIGN KEY (employee_id) REFERENCES employee (id),
  KEY fk_employee_hour_branchoffice_id (branchoffice_id),
  CONSTRAINT fk_employee_hour_branchoffice_id FOREIGN KEY (branchoffice_id) REFERENCES branchoffice (id)
  );