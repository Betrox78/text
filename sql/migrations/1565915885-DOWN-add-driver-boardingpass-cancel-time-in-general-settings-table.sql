-- 1565915885 DOWN add driver boardingpass cancel time in general settings table
DELETE FROM general_setting WHERE FIELD = 'driver_boardingpass_cancel_time' AND group_type = 'travel';