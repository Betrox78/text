-- 1594421957 UP fix-wrong-travel-date
update boarding_pass as bp
inner join boarding_pass_route as bpr on bp.id=bpr.boarding_pass_id
inner join schedule_route_destination as srd on srd.id=bpr.schedule_route_destination_id
set bp.travel_date=srd.travel_date
where bp.travel_date!=srd.travel_date and purchase_origin='app cliente' and DATE_FORMAT(bp.travel_date,'%H:%i:%s')='12:00:00';
