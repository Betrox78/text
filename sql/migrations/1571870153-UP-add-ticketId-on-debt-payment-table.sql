-- 1571870153 UP add-ticketId-on-debt-payment-table
ALTER TABLE debt_payment
    ADD COLUMN ticket_id INT(11) NULL AFTER debt_end;