-- 1573688275 UP add-menu-types-to-submodules
ALTER TABLE sub_module MODIFY COLUMN menu_type  ENUM('a_sub_catalogue', 'o_sub_catalogue', 'l_sub_config',
    'v_sub_vansrentalcost', 'c_sub_generalconfig', 'v_sub_vans', 'p_sub_parcel', 'r_sub_reports', 'r_sub_reportsales',
    'r_sub_reportinventory', 'r_sub_reportcashouts', 'r_sub_reportlogistics', 'r_sub_reportothers') NULL;

INSERT INTO sub_module (id, name, module_id, group_type, menu_type, created_by) VALUES
(80, 'app.reports.travels_canceled', 1, 'reports', 'r_sub_reportsales', 1),
(81, 'app.reports.parcels_canceled', 1, 'reports', 'r_sub_reportsales', 1),
(82, 'app.reports.24hrs_reservations', 1, 'reports', 'r_sub_reportsales', 1),
(83, 'app.reports.wallet', 1, 'reports', 'r_sub_reportsales', 1),

(84, 'app.reports.merchandise', 1, 'reports', 'r_sub_reportinventory', 1),
(85, 'app.reports.merchandise_in_transit', 1, 'reports', 'r_sub_reportinventory', 1),
(86, 'app.reports.pc_history', 1, 'reports', 'r_sub_reportinventory', 1),
(87, 'app.reports.contingency', 1, 'reports', 'r_sub_reportinventory', 1),

(88, 'app.reports.cash_outs', 1, 'reports', 'r_sub_reportcashouts', 1),
(89, 'app.reports.z_cash_out', 1, 'reports', 'r_sub_reportcashouts', 1),

(90, 'app.reports.maneuver', 1, 'reports', 'r_sub_reportlogistics', 1),
(91, 'app.reports.packages_scan', 1, 'reports', 'r_sub_reportlogistics', 1),
(92, 'app.reports.commercial_promise', 1, 'reports', 'r_sub_reportlogistics', 1),
(93, 'app.reports.commercial_promise.global', 1, 'reports', 'r_sub_reportlogistics', 1),
(94, 'app.reports.occupation', 1, 'reports', 'r_sub_reportlogistics', 1);

INSERT INTO permission (id, name, description, sub_module_id, created_by) VALUES
(182, '#list', 'Ver reporte', 80, 1),
(183, '#list', 'Ver reporte', 81, 1),
(184, '#list', 'Ver reporte', 82, 1),
(185, '#list', 'Ver reporte', 83, 1),

(186, '#list', 'Ver reporte', 84, 1),
(187, '#list', 'Ver reporte', 85, 1),
(188, '#list', 'Ver reporte', 86, 1),
(189, '#list', 'Ver reporte', 87, 1),

(190, '#list', 'Ver reporte', 88, 1),
(191, '#list', 'Ver reporte', 89, 1),

(192, '#list', 'Ver reporte', 90, 1),
(193, '#list', 'Ver reporte', 91, 1),
(194, '#list', 'Ver reporte', 92, 1),
(195, '#list', 'Ver reporte', 93, 1),
(196, '#list', 'Ver reporte', 94, 1);

UPDATE sub_module SET menu_type = 'r_sub_reportsales' WHERE id IN (51, 53, 54, 55, 77, 78, 79);
UPDATE sub_module SET menu_type = 'r_sub_reportinventory' WHERE id = 50;
UPDATE sub_module SET menu_type = 'r_sub_reportothers' WHERE id IN (66, 67);