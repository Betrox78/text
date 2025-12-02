-- 1709096408 UP add column percent discount applied parcel prepaid
ALTER TABLE parcels_prepaid
ADD COLUMN percent_discount_applied DECIMAL(5, 2) DEFAULT 0.00 AFTER total_amount;