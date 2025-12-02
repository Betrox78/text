-- 1682191558 UP schedule-route-destination-seat-lock

CREATE TABLE schedule_route_destination_seat_lock (
  id int(11) NOT NULL AUTO_INCREMENT,
  seat varchar(3) NOT NULL,
  integration_partner_session_id int(11) NOT NULL, 
  schedule_route_destination_id int(11) NOT NULL,
  status tinyint(4) DEFAULT '1',
  created_by int(11) NOT NULL,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  updated_by int(11) DEFAULT NULL,
  updated_at datetime DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_schedule_route_destination_id (schedule_route_destination_id),
  KEY idx_integration_partner_session_id (integration_partner_session_id),
  UNIQUE KEY idx_seat_schedule_route_destination_id_status (seat, schedule_route_destination_id, status),
  KEY idx_seat_partner_session_route_destination_status (seat, integration_partner_session_id, schedule_route_destination_id, status),
  CONSTRAINT fk_schedule_route_destination_id FOREIGN KEY (schedule_route_destination_id) REFERENCES schedule_route_destination (id),
  CONSTRAINT fk_integration_partner_session_id FOREIGN KEY (integration_partner_session_id) REFERENCES integration_partner_session (id)
)