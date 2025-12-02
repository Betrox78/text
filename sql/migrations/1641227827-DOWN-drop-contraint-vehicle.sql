-- 1641227827 DOWN drop-contraint-vehicle
ALTER TABLE vehicle
ADD  CONSTRAINT c_tipopermiso_ibfk_1 FOREIGN KEY(sat_permit_id) REFERENCES c_TipoPermiso(id);