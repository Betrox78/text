-- 1568392088 UP increase-billing-customer-name-lengh-to-255
ALTER TABLE customer_billing_information
    MODIFY COLUMN name VARCHAR(255) NOT NULL;