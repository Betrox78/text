-- 1558827344 UP add-travel-log
CREATE TABLE travel_logs (
id int(11) NOT NULL AUTO_INCREMENT,
travel_log_code varchar(50) NOT NULL,
schedule_route_id int(11) NOT NULL,
terminal_origin_id int(11) NOT NULL,
terminal_destiny_id int(11) NOT NULL,
load_id int(11) DEFAULT NULL,
download_id int(11) DEFAULT NULL,
origin enum('app-operation', 'operation') DEFAULT 'app-operation',
status enum('open', 'close') DEFAULT 'open',
PRIMARY KEY(id),
INDEX(travel_log_code),
KEY travel_logs_schedule_route_id_idx (schedule_route_id),
CONSTRAINT travel_logs_schedule_route_id_fk FOREIGN KEY (schedule_route_id) REFERENCES schedule_route (id),
KEY travel_logs_terminal_origin_id_idx (terminal_origin_id),
CONSTRAINT travel_logs_terminal_origin_id_fk FOREIGN KEY (terminal_origin_id) REFERENCES branchoffice (id),
KEY travel_logs_terminal_destiny_id_idx (terminal_destiny_id),
CONSTRAINT travel_logs_terminal_destiny_id_fk FOREIGN KEY (terminal_destiny_id) REFERENCES branchoffice (id),
KEY travel_logs_terminal_load_id_idx (load_id),
CONSTRAINT travel_logs_load_id_fk FOREIGN KEY (load_id) REFERENCES shipments (id),
KEY travel_logs_download_id_idx (load_id),
CONSTRAINT travel_logs_download_id_fk FOREIGN KEY (download_id) REFERENCES shipments (id)
)ENGINE=InnoDB CHARSET=utf8;