-- 1564085585 UP add columns in promos table for parcel service
ALTER TABLE promos
ADD COLUMN rule_for_packages enum('package_price', 'shipping') DEFAULT NULL AFTER rule,
ADD COLUMN type_packages enum('parcel', 'courier', 'all') DEFAULT NULL AFTER rule_for_packages,
ADD COLUMN apply_to_package_price varchar(20) DEFAULT NULL AFTER apply_to_special_tickets,
MODIFY COLUMN discount_type enum('direct_amount', 'direct_percent', 'free_n_product', 'discount_n_product', 'as_price') NOT NULL AFTER service;