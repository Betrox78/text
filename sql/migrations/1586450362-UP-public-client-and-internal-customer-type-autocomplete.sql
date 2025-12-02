-- 1586450362 UP public-client-and-internal-customer-type-autocomplete
UPDATE general_setting
SET type_field = 'autocomplete'
where FIELD IN ('public_client', 'internal_customer');