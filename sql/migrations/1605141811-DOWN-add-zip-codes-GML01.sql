-- 1605141811 DOWN add-zip-codes-GML01
delete from parcel_coverage where branchoffice_id = (select id from branchoffice where prefix = 'GML01');