-- 1697068047 DOWN change-cRazonSocial-billing-length
ALTER TABLE `CONTPAQ_ClientesProvedores`
CHANGE COLUMN `cRazonSocial` `cRazonSocial` VARCHAR(61) NULL DEFAULT NULL;