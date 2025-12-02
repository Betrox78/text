-- 1725145319 UP fix autoincrement parcels_confirm_dialogs_init_config
ALTER TABLE parcels_confirm_dialogs_init_config
DROP PRIMARY KEY;

ALTER TABLE parcels_confirm_dialogs_init_config
MODIFY COLUMN id INTEGER AUTO_INCREMENT PRIMARY KEY;