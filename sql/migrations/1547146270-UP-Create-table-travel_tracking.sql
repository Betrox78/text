-- 1547146270 UP Create table travel_tracking
CREATE TABLE `travel_tracking` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `schedule_route_id` int(11) NOT NULL,
  `schedule_route_destination_id` int(11) NOT NULL,
  `status` int(11) DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `travel_tracking_schedule_route_id_fk_idx` (`schedule_route_id`),
  KEY `travel_tracking_schedule_route_destination_id_fk_idx` (`schedule_route_destination_id`),
  CONSTRAINT `travel_tracking_schedule_route_destination_id_fk` FOREIGN KEY (`schedule_route_destination_id`) REFERENCES `schedule_route_destination` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `travel_tracking_schedule_route_id_fk` FOREIGN KEY (`schedule_route_id`) REFERENCES `schedule_route` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;