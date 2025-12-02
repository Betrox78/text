-- 1551141885 UP add-ticket-id-column-in-parcels-packages-tracking-table
ALTER TABLE parcels_packages_tracking
ADD COLUMN ticket_id int(11) DEFAULT NULL AFTER parcel_package_id,
ADD KEY `parcels_packages_tracking_ticket_idfk_idx` (`ticket_id`),
ADD CONSTRAINT `parcels_packages_tracking_ticket_id_fk` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;