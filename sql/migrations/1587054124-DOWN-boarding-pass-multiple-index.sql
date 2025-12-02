-- 1587054124 DOWN boarding-pass-multiple-index
DROP INDEX boarding_pass_status_idx ON boarding_pass;
DROP INDEX boarding_pass_boardingpass_status_idx ON boarding_pass;
DROP INDEX boarding_pass_created_at_idx ON boarding_pass;
DROP INDEX boarding_pass_travel_date_idx ON boarding_pass;
DROP INDEX boarding_pass_purchase_origin_idx ON boarding_pass;
DROP INDEX boarding_pass_payment_condition_idx ON boarding_pass;
DROP INDEX boarding_pass_terminal_origin_id_idx ON boarding_pass;
DROP INDEX boarding_pass_terminal_destiny_id_idx ON boarding_pass;
DROP INDEX boarding_pass_branchoffice_id_idx ON boarding_pass;