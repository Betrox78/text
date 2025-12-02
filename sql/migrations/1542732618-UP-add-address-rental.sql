-- 1542732618 UP add-address-rental

ALTER TABLE rental
ADD COLUMN address varchar(254) DEFAULT NULL AFTER email;

ALTER TABLE rental
ADD COLUMN credential_type enum('credential','license', 'passport', 'other') DEFAULT NULL AFTER address;