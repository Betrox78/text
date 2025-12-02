-- 1699073795 UP config branchoffices travel type table
CREATE TABLE config_branchoffices_travel_type(
	id INT(11) PRIMARY KEY AUTO_INCREMENT,
    terminal_origin_id INT(11) NOT NULL,
    terminal_destiny_id INT(11) NOT NULL,
    type_travel ENUM('local', 'foreign') NOT NULL DEFAULT 'local',
    status TINYINT(4) NOT NULL DEFAULT 1,
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_by INT(11) NOT NULL,
	updated_at DATETIME NULL DEFAULT NULL,
	updated_by INT(11) NULL DEFAULT NULL,
    UNIQUE INDEX config_branchoffices_travel_type_idx(terminal_origin_id, terminal_destiny_id),
    INDEX config_branchoffices_travel_type_search_idx(terminal_origin_id, terminal_destiny_id, type_travel),
    CONSTRAINT fk_terminal_origin_id FOREIGN KEY (terminal_origin_id) REFERENCES branchoffice(id) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT fk_terminal_destiny_id FOREIGN KEY (terminal_destiny_id) REFERENCES branchoffice(id) ON DELETE NO ACTION ON UPDATE NO ACTION
);