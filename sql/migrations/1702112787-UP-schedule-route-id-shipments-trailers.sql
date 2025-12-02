-- 1702112787 UP schedule route id shipments trailers
ALTER TABLE shipments_trailers
ADD COLUMN schedule_route_id INT(11) NOT NULL AFTER id,
ADD CONSTRAINT fk_schedule_route_id_shipments_trailers_idx FOREIGN KEY (schedule_route_id) REFERENCES schedule_route(id) ON DELETE NO ACTION ON UPDATE NO ACTION;