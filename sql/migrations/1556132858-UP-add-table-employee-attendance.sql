-- 1556132858 UP add-table-employee-attendance
CREATE TABLE employee_attendance (
  id INT NOT NULL AUTO_INCREMENT,
  employee_id INT NOT NULL,
  state ENUM('in', 'out') NOT NULL DEFAULT 'in',
  branchoffice_id INT NOT NULL,
  register_at DATETIME NOT NULL,
  status TINYINT(4) NOT NULL DEFAULT 1,
  created_by INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by INT NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY fk_employee_attendance_employee_id (employee_id),
  CONSTRAINT fk_employee_attendance_employee_id FOREIGN KEY (employee_id) REFERENCES employee (id),
  KEY fk_employee_attendance_branchoffice_id (branchoffice_id),
  CONSTRAINT fk_employee_attendance_branchoffice_id FOREIGN KEY (branchoffice_id) REFERENCES branchoffice (id)
);