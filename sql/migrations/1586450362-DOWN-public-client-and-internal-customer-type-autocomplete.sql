-- 1586450362 DOWN public-client-and-internal-customer-type-autocomplete
UPDATE general_setting
SET type_field = 'select'
where FIELD IN ('public_client', 'internal_customer');