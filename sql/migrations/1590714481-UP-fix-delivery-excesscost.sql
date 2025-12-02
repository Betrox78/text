-- 1590714481 UP fix-delivery-excesscost
update parcels as p set p.extra_charges=(select excess_cost from parcels_packages as pp where pp.parcel_id=10013) where p.id=10013;