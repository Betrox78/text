-- 1543274085 UP modify-action-tickets

ALTER TABLE tickets
MODIFY COLUMN action enum('purchase', 'income', 'change', 'cancel', 'expense', 'withdrawal', 'return') DEFAULT 'purchase';