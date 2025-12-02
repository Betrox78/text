update permission
set name='#Cancel.fast', description='Cancelacion al momento',created_at=now()
where  id = (select t1.id from
(select p.id from permission p
INNER JOIN sub_module sm ON sm.id = p.sub_module_id
where sm.name='app.parcel.cancellation' AND p.name = '#cancel') as t1);

insert into permission
(name,description,dependency_id,sub_module_id,multiple,status,created_at,created_by,updated_at,updated_by)
values ('#cancel.end','Cancelación extemporánea',null,70,0,1,now(),1,null,null);

insert into permission
(name,description,dependency_id,sub_module_id,multiple,status,created_at,created_by,updated_at,updated_by)
values ('#cancel.rework','Reexpedición',null,70,0,1,now(),1,null,null);

insert into permission
(name,description,dependency_id,sub_module_id,multiple,status,created_at,created_by,updated_at,updated_by)
values ('#cancel.return','Devolución',null,70,0,1,now(),1,null,null);

