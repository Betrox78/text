-- 1710940364 DOWN add-cp-hmo-parcel-coverage
delete from parcel_coverage where branchoffice_id in (select id from branchoffice where prefix = 'HMO01');