-- 1571870153 DOWN add-ticketId-on-debt-payment-table
ALTER TABLE debt_payment
    DROP COLUMN ticket_id;