-- 1589395157 UP clean-promos
UPDATE promos SET
rule_for_packages = NULL,
type_packages = NULL,
apply_to_package_price = NULL,
apply_to_package_price_distance = NULL
WHERE service = 'boardingpass';

UPDATE promos SET
apply_to_special_tickets = NULL,
rule = NULL,
apply_only_first_purchase = 0,
discount_per_base = 0,
apply_return = 0,
purchase_origin = 'sucursal'
WHERE service = 'parcel';

UPDATE promos SET
rule_for_packages = NULL,
type_packages = NULL,
apply_to_package_price = NULL,
apply_to_package_price_distance = NULL,
apply_to_special_tickets = NULL,
rule = NULL,
apply_only_first_purchase = 0,
discount_per_base = 0,
apply_return = 0,
purchase_origin = 'sucursal'
WHERE service = 'rental';
