-- 1552417261 UP create table auth_sub_services
CREATE TABLE auth_sub_services (
id int(11) NOT NULL AUTO_INCREMENT,
sub_route varchar(50) NOT NULL,
auth_service_id int(11) NOT NULL,
status int(11) NOT NULL DEFAULT 1,
created_by int(11) NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP(),
updated_by int(11) DEFAULT NULL,
updated_at datetime DEFAULT NULL,
PRIMARY KEY(id),
INDEX(sub_route),
KEY auth_sub_services_auth_service_id_idx (auth_service_id),
CONSTRAINT auth_sub_services_auth_service_id_fk FOREIGN KEY (auth_service_id) REFERENCES auth_services(id) ON DELETE CASCADE ON UPDATE CASCADE
)ENGINE=InnoDB CHARSET=utf8;