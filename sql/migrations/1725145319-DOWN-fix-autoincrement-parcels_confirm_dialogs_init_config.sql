-- 1725145319 DOWN fix autoincrement parcels_confirm_dialogs_init_config
ALTER TABLE parcels_confirm_dialogs_init_config
DROP PRIMARY KEY;

ALTER TABLE parcels_confirm_dialogs_init_config
MODIFY COLUMN id INTEGER PRIMARY KEY;