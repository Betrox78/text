-- 1753144706 UP add-service-subtype-col-to-invoice
ALTER TABLE `invoice`
  ADD COLUMN `service_subtype` ENUM('GC-CO', 'GPP-CO') NULL DEFAULT NULL AFTER service_type;