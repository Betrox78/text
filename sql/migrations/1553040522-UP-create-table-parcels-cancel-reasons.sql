-- 1553040522 UP create table parcels cancel reasons
CREATE TABLE parcels_cancel_reasons (
id int(11) NOT NULL AUTO_INCREMENT,
name varchar(50) NOT NULL,
responsable enum('customer', 'company', 'others') NOT NULL DEFAULT 'company',
cancel_type enum('fast_cancel', 'end_cancel', 'rework', 'return') NOT NULL DEFAULT 'fast_cancel',
status int(11) NOT NULL DEFAULT 1,
created_by int(11) NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP(),
updated_by int(11) DEFAULT NULL,
updated_at datetime DEFAULT NULL,
PRIMARY KEY(id)
)ENGINE=InnoDB CHARSET=utf8;