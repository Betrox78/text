-- 1587054124 UP boarding-pass-multiple-index
CREATE INDEX boarding_pass_status_idx ON boarding_pass(status);
CREATE INDEX boarding_pass_boardingpass_status_idx ON boarding_pass(boardingpass_status);
CREATE INDEX boarding_pass_created_at_idx ON boarding_pass(created_at);
CREATE INDEX boarding_pass_travel_date_idx ON boarding_pass(travel_date);
CREATE INDEX boarding_pass_purchase_origin_idx ON boarding_pass(purchase_origin);
CREATE INDEX boarding_pass_payment_condition_idx ON boarding_pass(payment_condition);
CREATE INDEX boarding_pass_terminal_origin_id_idx ON boarding_pass(terminal_origin_id);
CREATE INDEX boarding_pass_terminal_destiny_id_idx ON boarding_pass(terminal_destiny_id);
CREATE INDEX boarding_pass_branchoffice_id_idx ON boarding_pass(branchoffice_id);
