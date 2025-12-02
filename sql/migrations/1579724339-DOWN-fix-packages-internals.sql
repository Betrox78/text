-- 1579724339 DOWN fix-packages-internals
UPDATE parcels_packages pp
INNER JOIN parcels p ON p.id = pp.parcel_id
SET pp.discount = 0.00, pp.total_amount = pp.amount
WHERE p.customer_id = (select value from general_setting where FIELD = 'internal_customer')
AND p.is_internal_parcel IS TRUE;