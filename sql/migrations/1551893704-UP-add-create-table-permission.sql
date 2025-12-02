-- 1551893704 UP add create table permission
CREATE TABLE `permission` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`name` varchar(255) NOT NULL,
`sub_module_id` int(11) NOT NULL,
`multiple` tinyint(1) NOT NULL DEFAULT 0,
`status` tinyint(4) NOT NULL DEFAULT 1,
`created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
`created_by` int(11) NOT NULL,
`updated_at` datetime DEFAULT NULL,
`updated_by` int(11) DEFAULT NULL,
PRIMARY KEY(`id`),
KEY `permission_sub_module_id_idx` (`sub_module_id`),
CONSTRAINT `permission_sub_module_id_fk` FOREIGN KEY (`sub_module_id`) REFERENCES `sub_module` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB CHARSET=utf8;