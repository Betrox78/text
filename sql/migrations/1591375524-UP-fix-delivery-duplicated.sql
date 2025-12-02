-- 1591375524 UP fix-delivery-duplicated
-- Problem: The client use delivery contingency module to give entrence to the package and was delivered and after 
-- that they make the common process of arrival changing the parcel status to arrived
-- We´re leaving the tracking for further explanations
UPDATE parcels SET parcel_status=2 where id=9531;