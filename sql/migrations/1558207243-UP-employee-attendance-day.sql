-- 1558207243 UP employee-attendance-day
CREATE TABLE employee_attendance_day (
  id INT NOT NULL AUTO_INCREMENT,
  employee_id INT NOT NULL,
  employee_attendance_in_id INT NULL,
  employee_attendance_out_id INT NULL,
  attendance_day DATE NOT NULL,
  attendance_status ENUM('attendance', 'absence', 'late') NOT NULL DEFAULT 'attendance',
  hours TIME NOT NULL DEFAULT '00:00:00',
  status TINYINT(4) NOT NULL DEFAULT 1,
  created_by INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by INT NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY fk_employee_attendance_day_employee_id (employee_id),
  CONSTRAINT fk_employee_attendance_day_employee_id FOREIGN KEY (employee_id) REFERENCES employee (id),
  KEY fk_employee_attendance_day_employee_attendance_in_id (employee_attendance_in_id),
  CONSTRAINT fk_employee_attendance_day_employee_attendance_in_id FOREIGN KEY (employee_attendance_in_id) REFERENCES employee_attendance (id),
   KEY fk_employee_attendance_day_employee_attendance_out_id (employee_attendance_out_id),
  CONSTRAINT fk_employee_attendance_day_employee_attendance_out_id FOREIGN KEY (employee_attendance_out_id) REFERENCES employee_attendance (id)
);

ALTER TABLE employee
ADD COLUMN employee_attendance_day_id INT(11) DEFAULT NULL,
ADD INDEX employee_employee_attendance_day_id_fk_idx (employee_attendance_day_id ASC),
ADD CONSTRAINT employee_employee_attendance_day_id_fk_idx
  FOREIGN KEY (employee_attendance_day_id)
  REFERENCES employee_attendance_day (id)
  ON DELETE RESTRICT;
