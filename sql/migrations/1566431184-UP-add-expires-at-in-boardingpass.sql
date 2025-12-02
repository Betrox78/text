-- 1566431184 UP add-expires-at-in-boardingpass
ALTER TABLE boarding_pass
ADD COLUMN expires_at DATE NULL;
