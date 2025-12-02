-- 1696731731 UP fcm_token
CREATE TABLE fcm_token (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    token VARCHAR(255) NOT NULL,
    platform ENUM('Android', 'iOS', 'Web') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_valid TINYINT(1) DEFAULT 1,
    UNIQUE (token),
    FOREIGN KEY (customer_id) REFERENCES customer(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;