-- 1757511271 DOWN create-cfdi-module-perms
DELETE FROM `permission`
WHERE `sub_module_id` = 161
  AND `name` IN ('#credit_note', '#payment_complement');