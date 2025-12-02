-- 1668616256 UP submodule permissions shipment parcels
INSERT INTO sub_module(id, name, module_id, group_type, created_by) VALUES
(133, 'app.shipment_parcels_pending_to_load', 3, 'travels_log', 1),
(134, 'app.shipment_parcels_pending_to_download', 3, 'travels_log', 1);

INSERT INTO permission(id, name, description, dependency_id, sub_module_id, multiple, created_by) VALUES
(267, '#list', 'Ver lista', null, 133, 0, 1),
(268, '#list', 'Ver lista', null, 134, 0, 1),
(269, 'shipment_parcels_load', 'Iniciar embarque de paqueteria', 267, 133, 0, 1),
(270, 'shipment_parcels_download', 'Iniciar desembarque de paqueteria', 268, 134, 0, 1);