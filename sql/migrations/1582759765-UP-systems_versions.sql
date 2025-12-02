-- 1582759765 UP systems_versions
CREATE TABLE IF NOT EXISTS `systems_versions` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(40) DEFAULT NULL,
  `os` varchar(40) DEFAULT NULL,
  `version` varchar(5) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '2',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`Id`)
)