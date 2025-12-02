-- 1543274085 DOWN modify-action-tickets

ALTER TABLE tickets
MODIFY COLUMN action enum('purchase', 'income', 'change', 'cancel', 'expense', 'withdrawal') DEFAULT 'purchase';