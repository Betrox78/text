-- 1544579806 DOWN add-cancel-penalty-percent-in-general-setting
DELETE FROM general_setting where FIELD = 'cancel_penalty_parcel' and group_type = 'parcel';