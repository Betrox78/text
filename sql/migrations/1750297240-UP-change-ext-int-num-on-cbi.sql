-- 1750297240 UP change-ext-int-num-on-cbi
ALTER TABLE `customer_billing_information`
  MODIFY COLUMN `no_ext` VARCHAR(50) NOT NULL,
  MODIFY COLUMN `no_int` VARCHAR(50) NULL;

ALTER TABLE `CONTPAQ_ClientesProvedores`
  MODIFY COLUMN `cNumeroExterior` VARCHAR(50) DEFAULT NULL,
  MODIFY COLUMN `cNumeroInterior` VARCHAR(50) DEFAULT NULL;