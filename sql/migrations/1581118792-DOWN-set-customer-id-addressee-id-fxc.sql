-- 1581118792 DOWN set customer id addressee id fxc
UPDATE parcels SET customer_id = sender_id WHERE pays_sender IS FALSE ;