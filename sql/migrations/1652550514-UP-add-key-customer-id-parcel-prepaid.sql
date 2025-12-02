-- 1652550514 UP add-key-customer-id-parcel-prepaid
ALTER TABLE parcels_prepaid
ADD KEY `parcels_pp_customer_fk` (`customer_id`),
ADD CONSTRAINT `parcels_pp_customer_fk` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`);