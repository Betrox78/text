-- 1564085585 DOWN add columns in promos table for parcel service
ALTER TABLE promos
DROP COLUMN apply_to_package_price,
DROP COLUMN type_packages,
DROP COLUMN rule_for_packages,
MODIFY COLUMN discount_type enum('direct_amount', 'direct_percent', 'free_n_product', 'discount_n_product') NOT NULL AFTER service;