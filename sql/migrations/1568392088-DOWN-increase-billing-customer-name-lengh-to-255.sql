-- 1568392088 DOWN increase-billing-customer-name-lengh-to-255
ALTER TABLE customer_billing_information
    MODIFY COLUMN name VARCHAR(50) NOT NULL;