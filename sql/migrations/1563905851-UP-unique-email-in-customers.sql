-- 1563905851 UP unique-email-in-customers
CREATE UNIQUE INDEX customer_email_unique_idx
ON customer(email);