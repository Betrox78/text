-- 1562434921 UP update internal customer field in general setting
UPDATE general_setting SET explanation_text = 'customers?query=*,status=1' WHERE FIELD = 'internal_customer';