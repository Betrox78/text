-- 1546998629 DOWN insert-field-in-general-setting-for-active-and-desactive-the-frozen-charge-service
DELETE FROM general_setting WHERE FIELD = 'frozen_available_boarding' AND group_type = 'boarding';
DELETE FROM general_setting WHERE FIELD = 'frozen_available_parcel' AND group_type = 'parcel';