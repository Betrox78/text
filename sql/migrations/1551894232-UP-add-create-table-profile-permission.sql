-- 1551894232 UP add create table profile permission
CREATE TABLE `profile_permission` (
`profile_id` int(11) NOT NULL,
`permission_id` int(11) NOT NULL,
KEY `profile_permission_profile_id_idx` (`profile_id`),
CONSTRAINT `profile_permission_profile_id_fk` FOREIGN KEY (`profile_id`) REFERENCES `profile` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
KEY `profile_permission_permission_id_idx` (`permission_id`),
CONSTRAINT `profile_permission_permission_id_fk` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB CHARSET=utf8;