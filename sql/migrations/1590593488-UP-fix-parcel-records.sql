-- 1590593488 UP fix-parcel-records
delete from debt_payment where parcel_id=9175;
update parcels set debt = total_amount where id=9175;