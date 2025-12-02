-- 1543337511 UP add-expense-cash-out

ALTER TABLE cash_out_move
ADD COLUMN expense_id int(11) DEFAULT NULL AFTER payment_id;
ALTER TABLE cash_out_move ADD KEY fk_cash_out_move_expense_id (expense_id);
ALTER TABLE cash_out_move ADD CONSTRAINT fk_cash_out_move_expense_id FOREIGN KEY (expense_id) REFERENCES expense (id);