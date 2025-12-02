-- 1569006271 DOWN parcel delivery target in general settings was added
DELETE FROM general_setting WHERE FIELD = 'parcel_delivery_target' AND group_type = 'parcel';