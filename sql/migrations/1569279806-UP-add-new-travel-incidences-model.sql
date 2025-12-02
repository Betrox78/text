-- 1569279806 UP add-new-travel-incidences-model
CREATE TABLE IF NOT EXISTS travel_incidences(
    id INT(11) NOT NULL AUTO_INCREMENT,
    name VARCHAR(250) NOT NULL,
    description VARCHAR(254) NOT NULL,
    apply_to_service ENUM('boarding_pass', 'rental') NOT NULL DEFAULT 'boarding_pass',
    apply_to_driver TINYINT(1) NOT NULL DEFAULT 1,
    status TINYINT(4) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT(11) NOT NULL,
    updated_at DATETIME NULL,
    updated_by INT(11) NULL,
    PRIMARY KEY (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;
