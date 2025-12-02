-- 1551387784 UP create-parcels-deliveries-table
CREATE TABLE `parcels_deliveries` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`name` varchar(50) NOT NULL,
`last_name` varchar(50) NOT NULL,
`credential_type` enum('credential', 'license', 'passport', 'other') DEFAULT 'credential',
`no_credential` varchar(30) NOT NULL,
`signature` varchar(254) NOT NULL,
`status` int(11) NOT NULL DEFAULT 1,
`created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
`created_by` int(11) NOT NULL,
`updated_at` datetime DEFAULT NULL,
`updated_by` int(11) DEFAULT NULL,
PRIMARY KEY(`id`)
) ENGINE=InnoDB CHARSET=utf8;