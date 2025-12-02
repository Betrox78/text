-- 1570655616 UP add-cancel-phone-reservation-time-on-general-settings
INSERT INTO general_setting(FIELD, value, type_field, description, value_default, label_text, type_setting, group_type)
VALUES ('cancel_phone_reservation_time', '1', 'number', 'Horas para liberación de reservación', '1',
        'Horas para liberación de reservación', '0', 'travel');

ALTER TABLE boarding_pass
    MODIFY COLUMN expires_at DATETIME NULL;