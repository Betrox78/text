-- 1550958138 UP add-boarding-pass-tracking-table
CREATE TABLE `boarding_pass_tracking` (
  `boardingpass_id` int(11) NOT NULL,
`boardingpass_ticket_id` int(11) DEFAULT NULL,
`boardingpass_complement_id` int(11) DEFAULT NULL,
`ticket_id` int(11) DEFAULT NULL,
`notes` text,
`action` enum('created', 'changed-passenger', 'changed-date', 'changed-route', 'checkin', 'loaded', 'downloaded', 'canceled', 'printed', 'init-route', 'finished') NOT NULL,
`status` int(11) NOT NULL DEFAULT 1,
`created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
`created_by` int(11) NOT NULL,
KEY `boarding_pass_tracking_boardingpass_id_fk_idx` (`boardingpass_id`),
KEY `boarding_pass_tracking_boardingpass_ticket_id_fk_idx` (`boardingpass_ticket_id`),
KEY `boarding_pass_tracking_boardingpass_complement_idfk_idx` (`boardingpass_complement_id`),
CONSTRAINT `boarding_pass_tracking_boardingpass_id_fk` FOREIGN KEY (`boardingpass_id`) REFERENCES `boarding_pass` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
CONSTRAINT `boarding_pass_tracking_boardingpass_ticket_id_fk` FOREIGN KEY (`boardingpass_ticket_id`) REFERENCES `boarding_pass_ticket` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
CONSTRAINT `boarding_pass_tracking_boardingpass_complement_id_fk` FOREIGN KEY (`boardingpass_complement_id`) REFERENCES `boarding_pass_complement` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION,
CONSTRAINT `boarding_pass_tracking_ticket_id_fk` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB CHARSET=utf8;