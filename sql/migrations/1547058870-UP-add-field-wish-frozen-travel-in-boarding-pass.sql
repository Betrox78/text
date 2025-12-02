-- 1547058870 UP add-field-wish-frozen-travel-in-boarding-pass
ALTER TABLE boarding_pass
ADD COLUMN wish_frozen_travel tinyint(1) DEFAULT 0 NOT NULL;