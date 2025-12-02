-- 1697500323 DOWN referral_deeplink_foreign_key_change
ALTER TABLE deep_link_referral
DROP FOREIGN KEY deep_link_referral_ibfk_1;

ALTER TABLE deep_link_referral
CHANGE COLUMN e_wallet_id user_id INT NOT NULL,
ADD FOREIGN KEY (user_id) REFERENCES users(id);