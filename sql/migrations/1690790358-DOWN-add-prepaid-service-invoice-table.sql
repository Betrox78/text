-- 1690790358 DOWN add-prepaid-service-invoice-table
ALTER TABLE `invoice`
CHANGE COLUMN `service_type` `service_type` ENUM('boarding_pass', 'rental', 'parcel', 'freight', 'guia_pp') NOT NULL ;
