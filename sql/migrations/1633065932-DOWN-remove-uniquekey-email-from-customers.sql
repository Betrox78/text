-- 1633065932 DOWN remove-uniquekey-email-from-customers
CREATE UNIQUE INDEX customer_email_unique_idx
ON customer(email);