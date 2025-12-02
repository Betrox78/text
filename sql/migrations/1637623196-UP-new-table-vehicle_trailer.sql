-- 1637623196 UP new-table-vehicle_trailer

CREATE TABLE IF NOT EXISTS `vehicle_trailer` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `c_SubTipoRem_id` int(11) NOT NULL,
  `vehicle_id` int(11) NOT NULL,
  `plate` varchar(30) DEFAULT NULL,
  `Fecha_inicio_de_vigencia` date DEFAULT NULL,
  `Fecha_fin_de_vigencia` varchar(30) DEFAULT NULL,
  `status` int(11) NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY c_SubTipoRem_id_fk (c_SubTipoRem_id),
  KEY vehicle_id_fk (vehicle_id),
  CONSTRAINT c_SubTipoRem_id_fk FOREIGN KEY (c_SubTipoRem_id) REFERENCES c_SubTipoRem (id),
  CONSTRAINT vehicle_id_fk FOREIGN KEY (vehicle_id) REFERENCES vehicle (id)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=latin1;