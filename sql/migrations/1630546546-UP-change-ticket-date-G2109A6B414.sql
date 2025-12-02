-- 1630546546 UP change-ticket-date-G2109A6B414

update parcels set created_at = "2021-08-31 00:19:43" where parcel_tracking_code = "G2109A6B414";

update tickets set created_at = "2021-08-31 00:19:43" where parcel_id = (select id from parcels where parcel_tracking_code = "G2109A6B414");