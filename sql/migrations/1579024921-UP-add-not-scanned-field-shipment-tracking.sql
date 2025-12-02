-- 1579024921 UP add-not-scanned-field-shipment-tracking
ALTER TABLE shipments_ticket_tracking
    ADD COLUMN not_scanned boolean DEFAULT false;