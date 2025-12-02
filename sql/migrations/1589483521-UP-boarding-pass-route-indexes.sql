-- 1589483521 UP boarding-pass-route-indexes
CREATE INDEX boarding_pass_route_ticket_type_route_idx ON boarding_pass_route(ticket_type_route);
CREATE INDEX boarding_pass_route_order_route_idx ON boarding_pass_route(order_route);
CREATE INDEX boarding_pass_route_route_status_idx ON boarding_pass_route(route_status);
CREATE INDEX boarding_pass_route_status_idx ON boarding_pass_route(status);
CREATE INDEX boarding_pass_route_created_at_idx ON boarding_pass_route(created_at);

ALTER TABLE boarding_pass_route
ADD CONSTRAINT boarding_pass_route_config_destination_id_fk
  FOREIGN KEY (config_destination_id)
  REFERENCES config_destination (id);