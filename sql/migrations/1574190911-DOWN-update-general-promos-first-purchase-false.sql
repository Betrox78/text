-- 1574190911 DOWN update-general-promos-first-purchase-false
UPDATE general_setting SET explanation_text = 'promos?query=*,status=1,purchase_origin=4' WHERE FIELD = 'promo_app_cliente';
UPDATE general_setting SET explanation_text = 'promos?query=*,status=1,purchase_origin=2' WHERE FIELD = 'promo_web';