-- 1713170888 UP create logs tables
CREATE TABLE logs_transactions(
	id INT(11) PRIMARY KEY AUTO_INCREMENT,
    method VARCHAR(10) NOT NULL,
    path VARCHAR(80) NOT NULL,
    payload TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE logs_exceptions(
	id INT(11) PRIMARY KEY AUTO_INCREMENT,
    method VARCHAR(10) NOT NULL,
    path VARCHAR(80) NOT NULL,
    payload TEXT DEFAULT NULL,
    exception TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);