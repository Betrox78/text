-- 1548208485 UP migrate Add Column Shipping_type To Table Boarding_pass_complement
ALTER TABLE `boarding_pass_complement` 
ADD COLUMN `shipping_type` ENUM("baggage", "pets", "frozen") NULL DEFAULT 'baggage' AFTER `complement_id`;
