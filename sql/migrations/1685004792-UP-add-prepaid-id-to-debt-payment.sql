-- New column in debt payment
ALTER TABLE debt_payment
add column prepaid_travel_id int(11) DEFAULT NULL;

-- CREATE INDEX OF PREPAID_ID
CREATE INDEX debt_payment_prepaid_travel_id ON debt_payment(prepaid_travel_id);
