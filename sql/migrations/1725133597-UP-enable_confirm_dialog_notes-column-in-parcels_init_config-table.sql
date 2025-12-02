-- 1725133597 UP enable_confirm_dialog_notes column in parcels_init_config table
ALTER TABLE parcels_init_config
ADD COLUMN enable_confirm_dialog_notes BOOLEAN NOT NULL DEFAULT FALSE AFTER enable_send_whatsapp_notification;