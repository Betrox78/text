-- 1761618569 UP add-cbi-assigment-history-table
CREATE TABLE `cbi_assignment_history` (
  `id` int NOT NULL AUTO_INCREMENT,
  `parcel_id` int DEFAULT NULL,
  `parcel_prepaid_id` int DEFAULT NULL,
  `user_id` int NOT NULL,
  `prev_customer_billing_information_id` int DEFAULT NULL,
  `new_customer_billing_information_id` int DEFAULT NULL,
  `prev_rfc` varchar(13) DEFAULT NULL,
  `new_rfc` varchar(13) DEFAULT NULL,
  `move_type` enum('assign','re_assign') NOT NULL DEFAULT 'assign',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `cbi_assignment_history_parcel_id_idx` (`parcel_id`),
  KEY `cbi_assignment_history_parcel_prepaid_id_idx` (`parcel_prepaid_id`),
  KEY `cbi_assignment_history_user_id_idx` (`user_id`),
  KEY `cbi_assignment_history_prev_cbi_idx` (`prev_customer_billing_information_id`),
  KEY `cbi_assignment_history_new_cbi_idx` (`new_customer_billing_information_id`),
  KEY `cbi_assignment_history_prev_rfc_idx` (`prev_rfc`),
  KEY `cbi_assignment_history_new_rfc_idx` (`new_rfc`),
  KEY `cbi_assignment_history_move_type` (`move_type`),
  KEY `cbi_assignment_history_created_at` (`created_at`)
);