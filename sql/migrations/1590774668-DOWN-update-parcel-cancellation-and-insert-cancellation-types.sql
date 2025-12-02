update permission
set name='#cancel', description='Cancelación de paquetería',created_at='2019-05-27 17:44:09'
where  id = (select t1.id from
(select p.id from permission p
INNER JOIN sub_module sm ON sm.id = p.sub_module_id
where sm.name='app.parcel.cancellation' AND p.name = '#Cancel.fast') as t1);


delete p.* from permission p
inner join sub_module sm on sm.id=p.sub_module_id
where p.name in('#cancel.end','#cancel.rework','#cancel.return')
and sm.name = 'app.parcel.cancellation';