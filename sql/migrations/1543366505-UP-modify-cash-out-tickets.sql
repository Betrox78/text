-- 1543366505 UP modify-cash-out-tickets

ALTER TABLE tickets
MODIFY COLUMN cash_out_id int(11) DEFAULT NULL;