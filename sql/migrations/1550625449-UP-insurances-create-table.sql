-- 1550625449 UP insurances-create-table
CREATE TABLE `insurances` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
`policy_number` varchar(50) NOT NULL,
`insurance_carrier` varchar(50) NOT NULL,
`init` date NOT NULL,
`end` date NOT NULL,
`status` int(1) NOT NULL DEFAULT 1,
`created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
`created_by` int(11) DEFAULT NULL,
`updated_at` datetime DEFAULT NULL,
`updated_by` int(11) DEFAULT NULL,
PRIMARY KEY (`id`),
UNIQUE(`policy_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;