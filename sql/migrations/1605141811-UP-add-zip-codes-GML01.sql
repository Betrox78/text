-- 1605141811 UP add-zip-codes-GML01
insert into parcel_coverage (suburb_id , zip_code , branchoffice_id , created_by)
select distinct sub.id , sub.zip_code , (select id from branchoffice where prefix = 'GML01') as branchoffice_id , 1
from suburb sub
left join branchoffice b ON sub.id = b.suburb_id
where sub.county_id = (select county_id from branchoffice where prefix = 'GML01')
GROUP BY (sub.zip_code)
order by
sub.id,
sub.zip_code desc;