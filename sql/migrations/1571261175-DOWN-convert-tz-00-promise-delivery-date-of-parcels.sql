-- 1571261175 DOWN convert tz 00 promise delivery date of parcels
UPDATE parcels SET promise_delivery_date = CONVERT_TZ(promise_delivery_date, '+00:00', '-06:00');
