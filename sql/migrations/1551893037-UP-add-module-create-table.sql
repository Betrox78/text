-- 1551893037 UP add module-create-table
CREATE TABLE `module` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`name` varchar(255) NOT NULL,
`status` tinyint(4) NOT NULL DEFAULT 1,
`created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
`created_by` int(11) NOT NULL,
`updated_at` datetime DEFAULT NULL,
`updated_by` int(11) DEFAULT NULL,
PRIMARY KEY(`id`)
) ENGINE=InnoDB CHARSET=utf8;