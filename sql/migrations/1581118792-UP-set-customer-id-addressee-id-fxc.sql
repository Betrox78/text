-- 1581118792 UP set customer id addressee id fxc
UPDATE parcels SET customer_id = addressee_id WHERE pays_sender IS FALSE;