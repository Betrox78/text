-- 1543337511 DOWN add-expense-cash-out

ALTER TABLE cash_out_move
DROP COLUMN expense_id;
ALTER TABLE cash_out_move DROP CONSTRAINT fk_cash_out_move_expense_id;