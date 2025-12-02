-- 1691562453 UP create-sellers-table
CREATE TABLE `seller` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `last_name` varchar(255) NOT NULL,
  `branchoffice_id` INT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT '1',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NULL DEFAULT NULL,
  `created_by` int DEFAULT NULL,
  `updated_by` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT fk_seller_branchoffice_id FOREIGN KEY (branchoffice_id) REFERENCES branchoffice(id)
);