-- 1678155204 UP create-table-prepaid-package-segment
CREATE TABLE IF NOT EXISTS prepaid_package_segment(
id INT NOT NULL AUTO_INCREMENT,
prepaid_package_config_id int(11) NOT NULL,
terminal_origin_id int(11) NOT NULL COMMENT 'Id de la terminal de origen',
terminal_destiny_id int(11) NOT NULL COMMENT 'Id de la terminal de destino',
created_by int(11) DEFAULT NULL,
created_at datetime DEFAULT NOW(),
updated_by int(11) DEFAULT NULL,
updated_at datetime DEFAULT NULL,
PRIMARY KEY (`id`),
KEY prepaid_package_config_id_idx (prepaid_package_config_id),
CONSTRAINT prepaid_package_segment_config_id FOREIGN KEY (prepaid_package_config_id) REFERENCES prepaid_package_config (id) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8;