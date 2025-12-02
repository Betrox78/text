-- 1597089767 DOWN add-zip-codes-branchoffices
delete from parcel_coverage where branchoffice_id in (select id from branchoffice where branch_office_type = 'T');