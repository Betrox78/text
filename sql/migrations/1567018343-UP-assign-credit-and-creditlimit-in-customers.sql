-- 1567018343 UP assign-credit-and-creditlimit-in-customers
ALTER TABLE customer
CHANGE has_credit parcel_has_credit BOOLEAN DEFAULT FALSE,
CHANGE credit_limit parcel_credit_limit DECIMAL(12, 2) DEFAULT NULL,
CHANGE credit_time_limit parcel_credit_time_limit INTEGER(11) DEFAULT NULL;