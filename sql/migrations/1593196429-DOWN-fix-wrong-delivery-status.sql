-- 1593196429 DOWN fix-wrong-delivery-status
update parcels set parcel_status=10 where id=9539;
