-- 1690962273 DOWN fix-billingpending-perm
UPDATE `permission` SET `name` = '.pending', `description` = 'Facturación global' WHERE (`id` = '162');
DELETE FROM permission WHERE sub_module_id = 140;
DELETE FROM `sub_module` WHERE id = 140;
