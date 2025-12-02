

 CREATE TABLE `schedule_rad_ead` (
   `id` int(11) NOT NULL AUTO_INCREMENT,
   `idBranchOffice` int(11) NOT NULL ,
   `type_service` varchar(3) NOT NULL ,
   `privateSchedule` tinyint(1) NOT NULL DEFAULT '0',
   `sunSchedule` varchar(20)    DEFAULT '0' ,
   `monSchedule` varchar(20)   DEFAULT '0' ,
   `thuSchedule` varchar(20)   DEFAULT '0' ,
   `wenSchedule` varchar(20)   DEFAULT '0' ,
   `tueSchedule` varchar(20)   DEFAULT '0' ,
   `friSchedule` varchar(20)   DEFAULT '0' ,
   `satSchedule` varchar(20)   DEFAULT '0' ,

     `sun` tinyint(1) NOT NULL DEFAULT '0',
     `mon` tinyint(1) NOT NULL DEFAULT '0',
     `thu` tinyint(1) NOT NULL DEFAULT '0',
     `wen` tinyint(1) NOT NULL DEFAULT '0',
     `tue` tinyint(1) NOT NULL DEFAULT '0',
     `fri` tinyint(1) NOT NULL DEFAULT '0',
     `sat` tinyint(1) NOT NULL DEFAULT '0',
     `status` tinyint(4) NOT NULL DEFAULT '2',
   `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
   `created_by` int(11) DEFAULT NULL,
   `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   `updated_by` int(11) DEFAULT NULL,
   PRIMARY KEY (`Id`),
	  	FOREIGN KEY (`idBranchOffice`) REFERENCES `branchoffice` (`id`)
 )