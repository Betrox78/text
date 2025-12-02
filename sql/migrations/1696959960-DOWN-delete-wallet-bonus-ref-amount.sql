-- 1696959960 DOWN delete-wallet-bonus-ref-amount
INSERT INTO general_setting(FIELD, value, type_field, required_field, description, value_default, label_text, group_type, created_by)
VALUES ('bonus_by_e_wallet_referral', '1.0', 'currency', '1', 'Bonificación a monedero cuando un referido realiza su primer compra', '1', 'Bonificación por referido', 'e-wallet', '1');