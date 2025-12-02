-- 1720479499 DOWN reception v2.1 permissions and tables
DROP TABLE parcels_init_config;
DROP TABLE parcels_branchoffices_init_config;

DELETE FROM sub_module WHERE id = 149;
DELETE FROM permission WHERE sub_module_id = 149;