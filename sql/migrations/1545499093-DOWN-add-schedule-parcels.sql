-- 1545499093 DOWN add-schedule-parcels
ALTER TABLE parcels
DROP FOREIGN KEY `parcels_schedule_route_destination_id_fk`;
ALTER TABLE parcels
DROP COLUMN `schedule_route_destination_id`,
DROP INDEX `parcels_schedule_route_destination_id_fk`;