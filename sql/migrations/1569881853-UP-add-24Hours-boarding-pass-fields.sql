-- 1569881853 UP add-24Hours-boarding-pass-fields
INSERT INTO general_setting(FIELD, value, type_field, description, value_default, label_text, type_setting, group_type)
VALUES ('reservation_time', '24', 'number', 'Periodo de reservación', '24', 'Periodo de reservación', '0', 'travel');

INSERT INTO general_setting(FIELD, value, type_field, description, value_default, label_text, type_setting, group_type)
VALUES ('minimum_hour_reservation', '5', 'number', 'Hora mínima para reservación', '5', 'Hora mínima para reservación', '0', 'travel');

ALTER TABLE boarding_pass
    ADD COLUMN is_phone_reservation TINYINT(1) DEFAULT false;