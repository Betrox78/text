-- 1551895769 UP add create table user-permission-branchoffice
CREATE TABLE `user_permission_branchoffice` (
`user_id` int(11) NOT NULL,
`permission_id` int(11) NOT NULL,
`branchoffice_id` int(11) NOT NULL,
KEY `user_permission_branchoffice_user_id_idx` (`user_id`),
CONSTRAINT `user_permission_branchoffice_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
KEY `user_permission_branchoffice_permission_id_idx` (`permission_id`),
CONSTRAINT `user_permission_branchoffice_permission_id_fk` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
KEY `user_permission_branchoffice_branchoffice_id_idx` (`branchoffice_id`),
CONSTRAINT `user_permission_branchoffice_branchoffice_id_fk` FOREIGN KEY (`branchoffice_id`) REFERENCES `branchoffice` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB CHARSET=utf8;