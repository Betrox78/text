-- 1582155119 UP site_carrousel
CREATE TABLE `site_carusel` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `Descripcion` varchar(100) DEFAULT NULL,
  `orden` int(11) DEFAULT NULL,
  `Slide` longtext,
  `status` tinyint(4) NOT NULL DEFAULT '2',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `orden_UNIQUE` (`orden`)
)