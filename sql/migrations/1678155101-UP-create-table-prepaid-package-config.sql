-- 1678155101 UP create-table-prepaid-package-config
CREATE TABLE IF NOT EXISTS prepaid_package_config(
id INT NOT NULL AUTO_INCREMENT,
tickets_quantity int(11) NOT NULL DEFAULT 0 COMMENT 'Cantidad de boletos',
money decimal(12,2) NOT NULL DEFAULT 0.0 ,
apply_web tinyint(1) DEFAULT 0,
apply_app tinyint(1) DEFAULT 0,
apply_pos tinyint(1) DEFAULT 0,
created_by int(11) DEFAULT NULL,
created_at datetime DEFAULT NOW(),
updated_by int(11) DEFAULT NULL,
updated_at datetime DEFAULT NULL,
status tinyint(4) NOT NULL DEFAULT 1,
name varchar(60) NOT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;