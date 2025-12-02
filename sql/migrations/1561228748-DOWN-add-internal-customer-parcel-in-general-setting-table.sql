-- 1561228748 DOWN add-internal-customer-parcel-in-general-setting-table
DELETE FROM general_setting WHERE FIELD = 'internal_customer' AND group_type = 'parcel';