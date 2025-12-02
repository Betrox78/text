update permission 
set name='#cancel', description='Cancelar boletos',created_at='2019-07-16 19:22:44'
where  id = (select t1.id from
(select p.id from permission p 
INNER JOIN sub_module sm ON sm.id = p.sub_module_id 
where sm.name='app.reservation' AND p.name = '#Cancel.fast') as t1);

delete from permission where name='#cancel.end';