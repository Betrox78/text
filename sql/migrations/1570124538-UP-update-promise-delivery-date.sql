-- 1570124538 UP update promise delivery date
UPDATE parcels SET promise_delivery_date = DATE_ADD(DATE(created_at), INTERVAL '1 23' day_hour);