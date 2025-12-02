-- 1543859425 UP Create Pets-Sizes

CREATE TABLE `pets_sizes` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `animal_type` ENUM('dog', 'cat') NOT NULL DEFAULT 'dog',
  `height` DECIMAL(12,2) NOT NULL,
  `width` DECIMAL(12,2) NOT NULL,
  `length` DECIMAL(12,2) NOT NULL,
  `status` INT NOT NULL DEFAULT 1,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` INT NULL,
  `updated_at` DATETIME NULL,
  `updated_by` INT NULL,
  PRIMARY KEY (`id`));