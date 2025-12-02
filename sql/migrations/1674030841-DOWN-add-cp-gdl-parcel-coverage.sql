-- 1674030841 DOWN add-cp-gdl-parcel-coverage
delete from parcel_coverage where branchoffice_id in (select id from branchoffice where branch_office_type = 'T' AND prefix = 'GDL01');