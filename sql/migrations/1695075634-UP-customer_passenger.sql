-- 1695075634 UP customer_passenger
CREATE TABLE `customer_passenger` (
  `id` int NOT NULL AUTO_INCREMENT,
  `customer_id` int NOT NULL,
  `first_name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `gender` enum('m','f','n') NOT NULL,
  `birthday` date NOT NULL,
  `picture` varchar(50) NULL,
  `need_preferential` tinyint(1) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `customer_passenger_status_idx` (`status`),
  KEY `customer_passenger_created_at_idx` (`created_at`),
  CONSTRAINT `fk_customer_passenger_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
