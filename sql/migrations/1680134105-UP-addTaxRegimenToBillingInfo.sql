-- 1680134105 UP addTaxRegimenToBillingInfo
ALTER TABLE customer_billing_information
ADD COLUMN `tax_regimen` VARCHAR(10) NULL DEFAULT NULL AFTER contpaq_parcel_id;
