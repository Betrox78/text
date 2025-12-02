-- 1553676392 UP add-variable in action field in tickets table
ALTER TABLE tickets
MODIFY action enum('purchase', 'income', 'change', 'cancel', 'expense', 'withdrawal', 'return', 'voucher') DEFAULT NULL AFTER parcel_id;