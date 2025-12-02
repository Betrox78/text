-- 1551141885 DOWN add-ticket-id-column-in-parcels-packages-tracking-table
ALTER TABLE parcels_packages_tracking
DROP FOREIGN KEY `parcels_packages_tracking_ticket_id_fk`,
DROP COLUMN ticket_id;