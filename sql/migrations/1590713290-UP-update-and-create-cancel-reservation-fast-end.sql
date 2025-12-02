update permission
set name='#Cancel.fast', description='Cancelacion al momento',created_at=current_date()
where  id = (select t1.id from
(select p.id from permission p
INNER JOIN sub_module sm ON sm.id = p.sub_module_id
where sm.name='app.reservation' AND p.name = '#cancel') as t1);



insert into permission
(name,description,dependency_id,sub_module_id,multiple,status,created_at,created_by,updated_at,updated_by)
values ('#cancel.end','Cancelación extemporánea',110,39,0,1,current_date(),1,null,null);