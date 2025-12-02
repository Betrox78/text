-- 1552417317 UP create table permission_services
CREATE TABLE permission_services (
permission_id int(11) NOT NULL,
auth_service_id int(11) NOT NULL,
auth_sub_service_id int(11) DEFAULT NULL,
http_method enum('GET', 'POST', 'PUT', 'DELETE') NOT NULL DEFAULT 'GET',
INDEX(http_method),
KEY permission_services_permission_id_idx (permission_id),
CONSTRAINT permission_services_permission_id_fk FOREIGN KEY (auth_service_id) REFERENCES permission(id) ON DELETE CASCADE ON UPDATE CASCADE,
KEY permission_services_auth_service_id_idx (auth_service_id),
CONSTRAINT permission_services_auth_service_id_fk FOREIGN KEY (auth_service_id) REFERENCES auth_services(id) ON DELETE CASCADE ON UPDATE CASCADE,
KEY permission_services_auth_sub_service_id_idx (auth_sub_service_id),
CONSTRAINT permission_services_auth_sub_service_id_fk FOREIGN KEY (auth_sub_service_id) REFERENCES auth_sub_services(id) ON DELETE CASCADE ON UPDATE CASCADE
)ENGINE=InnoDB CHARSET=utf8;