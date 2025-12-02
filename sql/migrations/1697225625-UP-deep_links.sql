-- 1697225625 UP deep_links
CREATE TABLE deep_link_referral (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    link VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status TINYINT(1) DEFAULT 1,
    UNIQUE (link),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;