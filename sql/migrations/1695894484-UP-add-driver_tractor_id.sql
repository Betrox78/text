-- 1695894484 UP add driver_tractor_id
INSERT INTO `general_setting`
(`FIELD`,
`value`,
`type_field`,
`description`,
`value_default`,
`label_text`,
`explanation_text`,
`group_type`,
`status`,
`created_at`,
`created_by`)
VALUES
('driver_tractor_id',
'36',
'select',
'Puesto para asignar chofer a un tractocamion',
'36',
'Puesto de chofer de tractocamion',
'jobs?query=*,status=1',
'parcel',
1,
NOW(),
1);