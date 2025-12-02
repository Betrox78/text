-- 1653946093 UP distance_between_terminals
CREATE TABLE IF NOT EXISTS `package_terminals_distance` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `terminal_origin_id` int(11) NOT NULL,
  `terminal_destiny_id` int(11) NOT NULL,
  `travel_time` varchar(5) NOT NULL DEFAULT '00:00',
  `distance_km` decimal(12,2) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8;

INSERT INTO package_terminals_distance (terminal_origin_id, terminal_destiny_id, travel_time, distance_km, created_by)
VALUES
(1, 4, '05:25', 445.00,1),
(1, 5, '02:55', 225.00,1),
(1, 6, '00:55', 62.00,1),
(1, 10, '02:43', 209.00,1),
(1, 8, '00:30', 01.20,1),
(1, 9, '01:25', 105.00,1),

(4, 5, '02:43', 212.00,1),
(4, 6, '04:32', 350.00,1),
(4, 8, '05:16', 408.00,1),
(4, 9, '04:01', 315.00,1),
(4, 10, '02:44', 212.00,1),

(5, 6, '02:17', 150.00,1),
(5, 8, '02:39', 209.00,1),
(5, 9, '01:22', 106.00,1),
(5, 10, '00:04', 01.00,1),

(6, 8, '00:51', 62.09,1),
(6, 9, '00:39', 42.00,1),
(6, 10, '02:15', 149.00,1),

(8, 9, '01:21', 104.00,1),
(8, 10, '02:39', 208.00,1),

(9, 10, '01:37', 117.00,1),

(1, 1, '00:05', 01.00,1),
(2, 2, '00:05', 01.00,1),
(3, 3, '00:05', 01.00,1),
(4, 4, '00:05', 01.00,1),
(5, 5, '00:05', 01.00,1),
(6, 6, '00:05', 01.00,1),
(8, 8, '00:05', 01.00,1),
(9, 9, '00:05', 01.00,1),
(10, 10, '00:05', 01.00,1)
