-- 1545171464 DOWN add-general-setting-boarding-cancelation-time
DELETE FROM general_setting WHERE FIELD = 'boarding_cancelation_time' AND group_type = 'tickets';
