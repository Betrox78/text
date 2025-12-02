-- 1562796589 UP create-table-cubic
CREATE TABLE cubic (
id int(11) NOT NULL AUTO_INCREMENT,
name varchar(100) NOT NULL,
mac_address varchar(50) UNIQUE NOT NULL,
branchoffice_id int(11) NOT NULL,
status int(11) NOT NULL DEFAULT 1,
created_by int(11) NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP(),
updated_by int(11) DEFAULT NULL,
updated_at datetime DEFAULT NULL,
PRIMARY KEY(id),
INDEX(mac_address)
)ENGINE=InnoDB CHARSET=utf8;