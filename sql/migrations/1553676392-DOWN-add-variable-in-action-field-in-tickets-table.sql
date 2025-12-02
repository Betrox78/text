-- 1553676392 DOWN add-variable in action field in tickets table
ALTER TABLE tickets
MODIFY action enum('purchase', 'income', 'change', 'cancel', 'expense', 'withdrawal', 'return') DEFAULT NULL AFTER parcel_id;