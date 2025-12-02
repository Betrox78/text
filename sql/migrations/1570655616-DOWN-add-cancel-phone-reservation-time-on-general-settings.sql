-- 1570655616 DOWN add-cancel-phone-reservation-time-on-general-settings
DELETE FROM general_setting WHERE FIELD = 'cancel_phone_reservation_time' AND group_type = 'travel';
ALTER TABLE boarding_pass
    MODIFY COLUMN expires_at DATE NULL;