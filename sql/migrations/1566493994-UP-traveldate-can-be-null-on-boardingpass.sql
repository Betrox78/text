-- 1566493994 UP traveldate-can-be-null-on-boardingpass
ALTER TABLE boarding_pass
MODIFY travel_date DATETIME NULL;