-- 1691185678 UP e-wallet-structure
CREATE TABLE `e_wallet` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `available_amount` DECIMAL(12,2) NOT NULL DEFAULT '0.00',
  `referenced_by` INT NULL DEFAULT NULL,
  `code` VARCHAR(60) NULL DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT '1',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT fk_e_wallet_user_id FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_e_wallet_referenced_by FOREIGN KEY (referenced_by) REFERENCES users(id)
);


CREATE TABLE `e_wallet_recharge` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `e_wallet_id` int NOT NULL,
  `recharge_type` enum('purchase','promotion', 'bonus') NOT NULL DEFAULT 'purchase',
  `purchase_origin` enum('sucursal','web','kiosko','app cliente','app chofer') NOT NULL DEFAULT 'app cliente',
  `description` VARCHAR(255) NULL DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `payment_status` enum('pending','paid','cancelled','error') NOT NULL DEFAULT 'pending',
  `promo_id` int DEFAULT NULL,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `key_e_wallet_recharge_e_wallet_id` (`e_wallet_id`),
  CONSTRAINT fk_e_wallet_recharge_promo_id FOREIGN KEY (promo_id) REFERENCES promos(id),
  CONSTRAINT fk_e_wallet_recharge_e_wallet_id FOREIGN KEY (e_wallet_id) REFERENCES e_wallet(id)
);

CREATE TABLE `e_wallet_move` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `e_wallet_id` int NOT NULL,
  `move_type` enum('income','outcome') NOT NULL,
  `service_type` ENUM('boarding_pass', 'rental', 'parcel', 'freight', 'guia_pp', 'prepaid') DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `payment_id` int DEFAULT NULL,
  `e_wallet_recharge_id` int DEFAULT NULL,
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `key_e_wallet_move_e_wallet_id` (`e_wallet_id`),
  CONSTRAINT fk_e_wallet_move_e_wallet_id FOREIGN KEY (e_wallet_id) REFERENCES e_wallet(id),
  CONSTRAINT fk_e_wallet_move_payment_id FOREIGN KEY (payment_id) REFERENCES payment(id),
  CONSTRAINT fk_e_wallet_move_e_wallet_recharge_id FOREIGN KEY (e_wallet_recharge_id) REFERENCES e_wallet_recharge(id)
);

CREATE TABLE `e_wallet_recharges_range` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `min_range` decimal(12,2) NOT NULL DEFAULT '0.00',
  `max_range` decimal(12,2) NOT NULL DEFAULT '0.00',
  `extra_percent` decimal(12,2) NOT NULL DEFAULT '0.00',
  `status` TINYINT NOT NULL DEFAULT '1',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NULL DEFAULT NULL,
  `created_by` int DEFAULT NULL,
  `updated_by` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT fk_e_wallet_recharge_r_created_by FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT fk_e_wallet_recharge_r_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);
