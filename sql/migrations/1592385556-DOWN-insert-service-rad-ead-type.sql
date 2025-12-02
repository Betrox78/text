-- 1592385556 DOWN insert-service-rad-ead-type
update parcel_service_type set status = 0  where type_service = 'RAD'
update parcel_service_type set status = 0  where type_service = 'EAD'
update parcel_service_type set status = 0  where type_service = 'RAD-EAD'
