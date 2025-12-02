-- 1728456134 UP confirm dialog insurance config parcel v2.1
ALTER TABLE parcels_confirm_dialogs_init_config
ADD COLUMN enable_confirm_dialog_insurance BOOLEAN NOT NULL DEFAULT FALSE AFTER enable_confirm_dialog_notes;