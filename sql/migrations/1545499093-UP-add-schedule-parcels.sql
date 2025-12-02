-- 1545499093 UP add-schedule-parcels
ALTER TABLE parcels
ADD COLUMN schedule_route_destination_id int(11) DEFAULT NULL;

ALTER TABLE parcels
ADD CONSTRAINT parcels_schedule_route_destination_id_fk
  FOREIGN KEY (schedule_route_destination_id)
  REFERENCES schedule_route_destination (id);