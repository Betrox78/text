-- 1556040697 UP create table complement incidences
CREATE TABLE complement_incidences (
boarding_pass_complement_id int(11) NOT NULL,
incidence_id int(11) NOT NULL,
shipment_id int(11) NOT NULL,
notes text,
status int(1) DEFAULT 1 NOT NULL,
created_at datetime DEFAULT CURRENT_TIMESTAMP,
created_by int(11) DEFAULT NULL,
updated_at datetime DEFAULT NULL,
updated_by int(11) DEFAULT NULL,
KEY complement_incidences_boarding_pass_complement_id_idx (boarding_pass_complement_id),
CONSTRAINT complement_incidences_boarding_pass_complement_id_fk FOREIGN KEY (boarding_pass_complement_id) REFERENCES boarding_pass_complement(id) ON DELETE CASCADE ON UPDATE CASCADE,
KEY complement_incidences_incidence_id_idx (incidence_id),
CONSTRAINT complement_incidences_incidence_id_fk FOREIGN KEY (incidence_id) REFERENCES incidences(id) ON DELETE CASCADE ON UPDATE CASCADE,
KEY complement_incidences_shipment_id_idx (shipment_id),
CONSTRAINT complement_incidences_shipment_id_fk FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE ON UPDATE CASCADE
)ENGINE=InnoDB CHARSET=utf8;