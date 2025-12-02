-- 1562434921 DOWN update internal customer field in general setting
UPDATE general_setting SET explanation_text = '' WHERE FIELD = 'internal_customer';