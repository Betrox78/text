-- 1697500323 UP referral_deeplink_foreign_key_change
ALTER TABLE deep_link_referral
DROP FOREIGN KEY deep_link_referral_ibfk_1;

ALTER TABLE deep_link_referral
CHANGE COLUMN user_id e_wallet_id INT NOT NULL,
ADD FOREIGN KEY (e_wallet_id) REFERENCES e_wallet(id);