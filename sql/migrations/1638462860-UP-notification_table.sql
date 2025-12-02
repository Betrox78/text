-- 1638462860 UP notification_table
CREATE TABLE IF NOT EXISTS notification (
  id INT AUTO_INCREMENT PRIMARY KEY,
  notification_action VARCHAR(100) NOT NULL,
  platform VARCHAR(255) NOT NULL,
  id_resource BIGINT NOT NULL,
  flag_resource VARCHAR(100) NOT NULL,
  status INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
