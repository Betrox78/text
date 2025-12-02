-- 1569881853 DOWN add-24Hours-boarding-pass-fields
DELETE FROM general_setting WHERE FIELD = 'reservation_time' AND group_type = 'travel';
DELETE FROM general_setting WHERE FIELD = 'minimum_hour_reservation' AND group_type = 'travel';

ALTER TABLE boarding_pass
    DROP COLUMN is_phone_reservation;