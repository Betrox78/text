-- 1637610741 UP parcel-whatsapp-notifications
ALTER TABLE parcels ADD send_whatsapp_notification TINYINT DEFAULT 0 AFTER in_payment;
