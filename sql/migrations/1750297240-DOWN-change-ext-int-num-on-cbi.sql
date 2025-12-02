-- 1750297240 DOWN change-ext-int-num-on-cbi
ALTER TABLE `customer_billing_information`
  MODIFY COLUMN `no_ext` VARCHAR(15) NOT NULL,
  MODIFY COLUMN `no_int` VARCHAR(15) NULL;

ALTER TABLE `CONTPAQ_ClientesProvedores`
  MODIFY COLUMN `cNumeroExterior` VARCHAR(10) DEFAULT NULL,
  MODIFY COLUMN `cNumeroInterior` VARCHAR(10) DEFAULT NULL;