-- 1551837691 UP add-create table-users
CREATE TABLE `users` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`name` varchar(255) NOT NULL,
`email` varchar(120) NOT NULL,
`phone` char(10) NOT NULL,
`pass` varchar(255) NOT NULL,
`blocked` tinyint(1) NOT NULL DEFAULT 0,
`blocked_until_date` datetime DEFAULT NULL,
`user_type` enum('A', 'C') NOT NULL DEFAULT 'A',
`profile_id` int(11) DEFAULT NULL,
`status` tinyint(4) NOT NULL DEFAULT 1,
`created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
`created_by` int(11) NOT NULL,
`updated_at` datetime DEFAULT NULL,
`updated_by` int(11) DEFAULT NULL,
PRIMARY KEY(`id`),
KEY `users_profile_id_idx` (`profile_id`),
CONSTRAINT `users_profile_id_fk` FOREIGN KEY (`profile_id`) REFERENCES `profile` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB CHARSET=utf8;