-- 1551893235 UP add create table sub module
CREATE TABLE `sub_module` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`name` varchar(255) NOT NULL,
`module_id` int(11) NOT NULL,
`status` tinyint(4) NOT NULL DEFAULT 1,
`created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
`created_by` int(11) NOT NULL,
`updated_at` datetime DEFAULT NULL,
`updated_by` int(11) DEFAULT NULL,
PRIMARY KEY(`id`),
KEY `sub_module_module_id_idx` (`module_id`),
CONSTRAINT `sub_module_module_id_fk` FOREIGN KEY (`module_id`) REFERENCES `module` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB CHARSET=utf8;