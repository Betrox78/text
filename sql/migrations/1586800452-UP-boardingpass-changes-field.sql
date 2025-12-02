-- 1586800452 UP boardingpass-changes-field
ALTER TABLE boarding_pass
  ADD changes int default 0;