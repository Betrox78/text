-- 1552417148 UP create table auth_services
CREATE TABLE auth_services (
id int(11) NOT NULL AUTO_INCREMENT,
route varchar(50) UNIQUE NOT NULL,
status int(11) NOT NULL DEFAULT 1,
created_by int(11) NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP(),
updated_by int(11) DEFAULT NULL,
updated_at datetime DEFAULT NULL,
PRIMARY KEY(id),
INDEX(route)
)ENGINE=InnoDB CHARSET=utf8;