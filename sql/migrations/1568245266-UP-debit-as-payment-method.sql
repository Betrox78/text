-- 1568245266 UP debit-as-payment-method
ALTER TABLE payment
MODIFY COLUMN payment_method enum('cash','card','check','transfer','deposit', 'debit');
