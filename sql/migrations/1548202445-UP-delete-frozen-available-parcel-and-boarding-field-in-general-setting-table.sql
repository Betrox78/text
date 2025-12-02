-- 1548202445 UP delete frozen-available-parcel-and-boarding-field-in-general-setting-table
DELETE FROM general_setting WHERE FIELD = 'frozen_available_boarding' AND group_type = 'boarding';
DELETE FROM general_setting WHERE FIELD = 'frozen_available_parcel' AND group_type = 'parcel';