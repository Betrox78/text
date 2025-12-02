-- 1597089767 UP add-zip-codes-branchoffices
-- Los Mochis
delete from parcel_coverage where branchoffice_id in (select id from branchoffice where branch_office_type = 'T');

insert into parcel_coverage (suburb_id , zip_code , branchoffice_id , created_by)
select distinct sub.id , sub.zip_code , (select id from branchoffice where prefix = 'LMM01') as branchoffice_id , 1
from suburb sub
left join branchoffice b ON sub.id = b.suburb_id
where sub.county_id = (select county_id from branchoffice where prefix = 'LMM01')
GROUP BY (sub.zip_code)
order by
sub.id,
sub.zip_code desc;

insert into parcel_coverage (suburb_id , zip_code , branchoffice_id , created_by)
select distinct sub.id , sub.zip_code , (select id from branchoffice where prefix = 'GVE01') as branchoffice_id , 1
from suburb sub
left join branchoffice b ON sub.id = b.suburb_id
where sub.county_id = (select county_id from branchoffice where prefix = 'GVE01')
GROUP BY (sub.zip_code)
order by
sub.id,
sub.zip_code desc;

insert into parcel_coverage (suburb_id , zip_code , branchoffice_id , created_by)
select distinct sub.id , sub.zip_code , (select id from branchoffice where prefix = 'CUL01') as branchoffice_id , 1
from suburb sub
left join branchoffice b ON sub.id = b.suburb_id
where sub.county_id = (select county_id from branchoffice where prefix = 'CUL01')
GROUP BY (sub.zip_code)
order by
sub.id,
sub.zip_code desc;


insert into parcel_coverage (suburb_id , zip_code , branchoffice_id , created_by)
select distinct sub.id , sub.zip_code , (select id from branchoffice where prefix = 'MZT01') as branchoffice_id , 1
from suburb sub
left join branchoffice b ON sub.id = b.suburb_id
where sub.county_id = (select county_id from branchoffice where prefix = 'MZT01')
GROUP BY (sub.zip_code)
order by
sub.id,
sub.zip_code desc;