-- 1562801613 UP create-table-cubic-log
-- 1552417148 UP create table auth_services
CREATE TABLE cubic_log (
id int(11) NOT NULL AUTO_INCREMENT,
cubic_id int(11) NOT NULL,
branchoffice_id int(11) NOT NULL,
cubic_log_status ENUM('ok','busy','error') DEFAULT 'ok' NOT NULL,
image varchar(100),
length decimal(12,2) NOT NULL DEFAULT 0.0,
width decimal(12,2) NOT NULL DEFAULT 0.0,
height decimal(12,2) NOT NULL DEFAULT 0.0,
weight decimal(12,2) NOT NULL DEFAULT 0.0,
status int(11) NOT NULL DEFAULT 1,
created_by int(11) NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP(),
updated_by int(11) DEFAULT NULL,
updated_at datetime DEFAULT NULL,
PRIMARY KEY(id),
INDEX(cubic_id),
CONSTRAINT fk_cubiclog_cubic_id FOREIGN KEY (cubic_id) REFERENCES cubic(id)
)ENGINE=InnoDB CHARSET=utf8;