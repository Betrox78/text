-- 1579724339 UP fix-packages-internals
UPDATE parcels_packages pp
INNER JOIN parcels p ON p.id = pp.parcel_id
SET pp.discount = pp.amount, pp.total_amount = 0.00
WHERE p.customer_id = (select value from general_setting where FIELD = 'internal_customer')
AND p.is_internal_parcel IS TRUE;