-- 1559873583 UP cashouts permissions

INSERT INTO sub_module (id,name,module_id,group_type,menu_type,created_by) 
VALUES (72, 'app.reports.cash_outs.detail', 3, 'reports', 'r_sub_reports', 1);


INSERT INTO permission (id, name, description,sub_module_id,dependency_id, created_by) 
  VALUES (169,'#list', 'Ver listado de reporte de caja',72,null,1),
        (170,'.detail', 'Ver detalle de reporte de caja',72,169,1);
