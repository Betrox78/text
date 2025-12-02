-- 1697068047 UP change-cRazonSocial-billing-length
ALTER TABLE `CONTPAQ_ClientesProvedores`
CHANGE COLUMN `cRazonSocial` `cRazonSocial` VARCHAR(250) NULL DEFAULT NULL;