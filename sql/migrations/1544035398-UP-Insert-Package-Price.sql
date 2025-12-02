-- 1544035398 UP Insert-Package-Price
ALTER TABLE `package_price` 
CHANGE COLUMN `created_ by` `created_by` INT(11) NULL DEFAULT NULL ;

INSERT INTO `package_price` (name_price, min_linear_volume, max_linear_volume, min_weight, max_weight, price, currency_id, parent_id, shipping_type, status, created_at, created_by, updated_at, updated_by) VALUES 
('FT0',0.00,0.09,0.00,0.09,0.00,(SELECT value FROM general_setting WHERE FIELD='currency_id'),NULL,'frozen',1,current_timestamp(),1,NULL,NULL),
('FT1',0.10,10.99,0.10,32.99,0.20,(SELECT value FROM general_setting WHERE FIELD='currency_id'),NULL,'frozen',1,current_timestamp(),1,NULL,NULL),
('FT2',11.00,20.99,33.00,64.99,0.60,(SELECT value FROM general_setting WHERE FIELD='currency_id'),NULL,'frozen',1,current_timestamp(),1,NULL,NULL),
('FT3',21.00,30.99,65.00,96.99,1.20,(SELECT value FROM general_setting WHERE FIELD='currency_id'),NULL,'frozen',1,current_timestamp(),1,NULL,NULL),
('FT4',31.00,40.99,97.00,128.99,1.80,(SELECT value FROM general_setting WHERE FIELD='currency_id'),NULL,'frozen',1,current_timestamp(),1,NULL,NULL),
('FT5',41.00,9999.99,129.00,9999.99,2.00,(SELECT value FROM general_setting WHERE FIELD='currency_id'),NULL,'frozen',1,current_timestamp(),1,NULL,NULL);