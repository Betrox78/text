-- 1567447216 UP revert-assign-credit-and-creditLimit
ALTER TABLE customer
CHANGE parcel_has_credit has_credit BOOLEAN DEFAULT FALSE,
CHANGE parcel_credit_limit credit_limit DECIMAL(12, 2) DEFAULT NULL,
CHANGE parcel_credit_time_limit credit_time_limit INTEGER(11) DEFAULT NULL;