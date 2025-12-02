-- 1633065932 UP remove-uniquekey-email-from-customers
ALTER TABLE customer
DROP INDEX customer_email_unique_idx;