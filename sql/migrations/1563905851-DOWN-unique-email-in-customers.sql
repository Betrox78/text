-- 1563905851 DOWN unique-email-in-customers
ALTER TABLE customer
DROP INDEX customer_email_unique_idx;