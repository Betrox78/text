-- 1574190911 UP update-general-promos-first-purchase-false
UPDATE general_setting SET explanation_text = 'promos?query=*,status=1,purchase_origin=4,apply_only_first_purchase=false' WHERE FIELD = 'promo_app_cliente';
UPDATE general_setting SET explanation_text = 'promos?query=*,status=1,purchase_origin=2,apply_only_first_purchase=false' WHERE FIELD = 'promo_web';