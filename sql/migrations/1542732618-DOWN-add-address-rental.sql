-- 1542732618 DOWN add-address-rental

ALTER TABLE rental
DROP COLUMN address;

ALTER TABLE rental
DROP COLUMN credential_type;