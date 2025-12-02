-- MySQL dump 10.13  Distrib 5.7.24, for Linux (x86_64)
--
-- Host: localhost    Database: abordo_test
-- ------------------------------------------------------
-- Server version	5.7.24-0ubuntu0.18.10.1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
--
--

--
-- Table structure for table `addon_serial`
--

DROP TABLE IF EXISTS `addon_serial`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `addon_serial` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `addon_vehicle_id` int(11) NOT NULL,
  `serial_num` varchar(50) NOT NULL,
  `last_maintenance` date DEFAULT NULL,
  `rent_status` int(11) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_addon_serial_addon_vehicle_id` (`addon_vehicle_id`),
  CONSTRAINT `fk_addon_serial_addon_vehicle_id` FOREIGN KEY (`addon_vehicle_id`) REFERENCES `addon_vehicle` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `addon_serial`
--

LOCK TABLES `addon_serial` WRITE;
/*!40000 ALTER TABLE `addon_serial` DISABLE KEYS */;
/*!40000 ALTER TABLE `addon_serial` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `addon_vehicle`
--

DROP TABLE IF EXISTS `addon_vehicle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `addon_vehicle` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `prefix_sku` varchar(10) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(254) DEFAULT NULL,
  `file_info` varchar(254) DEFAULT NULL,
  `is_an_extra` tinyint(1) NOT NULL DEFAULT '0',
  `cost` decimal(12,2) NOT NULL DEFAULT '0.00',
  `use_price` decimal(12,2) NOT NULL DEFAULT '0.00',
  `lost_price` decimal(12,2) DEFAULT NULL,
  `total_addons` int(11) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `removable` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `addon_vehicle`
--

LOCK TABLES `addon_vehicle` WRITE;
/*!40000 ALTER TABLE `addon_vehicle` DISABLE KEYS */;
/*!40000 ALTER TABLE `addon_vehicle` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `boarding_pass`
--

DROP TABLE IF EXISTS `boarding_pass`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `boarding_pass` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `travel_date` datetime NOT NULL,
  `travel_return_date` datetime DEFAULT NULL,
  `ticket_type` enum('abierto','sencillo','redondo') NOT NULL,
  `customer_id` int(11) DEFAULT NULL,
  `seatings` int(11) NOT NULL DEFAULT '1',
  `reservation_code` varchar(60) DEFAULT NULL,
  `webid` int(11) DEFAULT NULL,
  `has_invoice` tinyint(1) NOT NULL DEFAULT '0',
  `num_invoice` varchar(20) DEFAULT NULL,
  `exchange_rate_id` int(11) DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `discount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `purchase_origin` enum('plataforma','web','kiosko','app cliente','app chofer') NOT NULL,
  `discount_code_id` int(11) DEFAULT NULL,
  `principal_passenger_id` int(11) DEFAULT NULL,
  `boardingpass_status` int(11) NOT NULL DEFAULT '4',
  `conekta_order_id` varchar(25) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `iva` decimal(12,2) DEFAULT '0.00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_reservation_code_status` (`reservation_code`,`status`),
  KEY `fk_boarding_pass_customer` (`customer_id`),
  KEY `fk_boarding_pass_exchange_rate` (`exchange_rate_id`),
  KEY `fk_boarding_pass_principal_passenger_idx` (`principal_passenger_id`),
  CONSTRAINT `fk_boarding_pass_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`),
  CONSTRAINT `fk_boarding_pass_exchange_rate` FOREIGN KEY (`exchange_rate_id`) REFERENCES `exchange_rate` (`id`),
  CONSTRAINT `fk_boarding_pass_principal_passenger` FOREIGN KEY (`principal_passenger_id`) REFERENCES `boarding_pass_passenger` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `boarding_pass`
--

LOCK TABLES `boarding_pass` WRITE;
/*!40000 ALTER TABLE `boarding_pass` DISABLE KEYS */;
/*!40000 ALTER TABLE `boarding_pass` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `boarding_pass_complement`
--

DROP TABLE IF EXISTS `boarding_pass_complement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `boarding_pass_complement` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `boarding_pass_id` int(11) NOT NULL,
  `boarding_pass_ticket_id` int(11) NOT NULL,
  `complement_id` int(11) NOT NULL,
  `weight` decimal(12,2) DEFAULT NULL,
  `height` decimal(12,2) DEFAULT NULL,
  `width` decimal(12,2) DEFAULT NULL,
  `length` decimal(12,2) DEFAULT NULL,
  `tracking_code` varchar(50) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fkey_boarding_pass_complement_boarding_pass` (`boarding_pass_id`),
  KEY `fkey_boarding_pass_complement_complement` (`complement_id`),
  KEY `fk_boarding_pass_complement_ticket` (`boarding_pass_ticket_id`),
  CONSTRAINT `fk_boarding_pass_complement_ticket` FOREIGN KEY (`boarding_pass_ticket_id`) REFERENCES `boarding_pass_ticket` (`id`),
  CONSTRAINT `fkey_boarding_pass_complement_boarding_pass` FOREIGN KEY (`boarding_pass_id`) REFERENCES `boarding_pass` (`id`),
  CONSTRAINT `fkey_boarding_pass_complement_complement` FOREIGN KEY (`complement_id`) REFERENCES `complement` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `boarding_pass_complement`
--

LOCK TABLES `boarding_pass_complement` WRITE;
/*!40000 ALTER TABLE `boarding_pass_complement` DISABLE KEYS */;
/*!40000 ALTER TABLE `boarding_pass_complement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `boarding_pass_passenger`
--

DROP TABLE IF EXISTS `boarding_pass_passenger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `boarding_pass_passenger` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `boarding_pass_id` int(11) NOT NULL,
  `first_name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `gender` enum('m','f','n') NOT NULL,
  `birthday` date NOT NULL,
  `special_ticket_id` int(11) NOT NULL,
  `need_preferential` tinyint(1) NOT NULL,
  `principal_passenger` tinyint(1) NOT NULL DEFAULT '0',
  `is_customer` tinyint(1) NOT NULL DEFAULT '0',
  `is_child_under_age` tinyint(1) NOT NULL DEFAULT '0',
  `parent_id` int(11) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_boarding_pass_passanger_boarding_pass` (`boarding_pass_id`),
  KEY `fk_boarding_pass_passanger_special_ticket` (`special_ticket_id`),
  CONSTRAINT `fk_boarding_pass_passanger_boarding_pass` FOREIGN KEY (`boarding_pass_id`) REFERENCES `boarding_pass` (`id`),
  CONSTRAINT `fk_boarding_pass_passanger_special_ticket` FOREIGN KEY (`special_ticket_id`) REFERENCES `special_ticket` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `boarding_pass_passenger`
--

LOCK TABLES `boarding_pass_passenger` WRITE;
/*!40000 ALTER TABLE `boarding_pass_passenger` DISABLE KEYS */;
/*!40000 ALTER TABLE `boarding_pass_passenger` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `boarding_pass_route`
--

DROP TABLE IF EXISTS `boarding_pass_route`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `boarding_pass_route` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `boarding_pass_id` int(11) NOT NULL,
  `schedule_route_destination_id` int(11) NOT NULL,
  `order_route` int(11) NOT NULL DEFAULT '1',
  `ticket_type_route` enum('ida','regreso') NOT NULL DEFAULT 'ida',
  `route_status` tinyint(4) NOT NULL DEFAULT '1',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_boarding_pass_route_boarding_pass` (`boarding_pass_id`),
  KEY `fk_boarding_pass_schedule_route_idx` (`schedule_route_destination_id`),
  CONSTRAINT `fk_boarding_pass_route_boarding_pass` FOREIGN KEY (`boarding_pass_id`) REFERENCES `boarding_pass` (`id`),
  CONSTRAINT `fk_boarding_pass_route_destination` FOREIGN KEY (`schedule_route_destination_id`) REFERENCES `schedule_route_destination` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `boarding_pass_route`
--

LOCK TABLES `boarding_pass_route` WRITE;
/*!40000 ALTER TABLE `boarding_pass_route` DISABLE KEYS */;
/*!40000 ALTER TABLE `boarding_pass_route` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `boarding_pass_ticket`
--

DROP TABLE IF EXISTS `boarding_pass_ticket`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `boarding_pass_ticket` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `boarding_pass_passenger_id` int(11) NOT NULL,
  `boarding_pass_route_id` int(11) NOT NULL,
  `config_ticket_price_id` int(11) NOT NULL,
  `seat` varchar(3) NOT NULL,
  `rfid_1` varchar(100) DEFAULT NULL,
  `rfid_2` varchar(100) DEFAULT NULL,
  `cost` decimal(12,2) DEFAULT '0.00',
  `amount` decimal(12,2) DEFAULT '0.00',
  `extra_charges` decimal(12,2) DEFAULT '0.00',
  `extra_weight` decimal(6,2) DEFAULT '0.00',
  `extra_linear_volume` decimal(6,2) DEFAULT '0.00',
  `discount` decimal(12,2) DEFAULT '0.00',
  `total_amount` decimal(12,2) DEFAULT '0.00',
  `check_in` tinyint(1) DEFAULT '0',
  `checkedin_at` datetime DEFAULT NULL,
  `has_complements` tinyint(1) DEFAULT '0',
  `was_printed` tinyint(1) DEFAULT '0',
  `printed_at` datetime DEFAULT NULL,
  `ticket_status` tinyint(1) DEFAULT '1',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`,`boarding_pass_passenger_id`),
  KEY `fk_boarding_pass_ticket_passenger_idx` (`boarding_pass_passenger_id`),
  KEY `fk_boarding_pass_ticket_route_idx` (`boarding_pass_route_id`),
  KEY `fk_boarding_pass_ticket_price_idx` (`config_ticket_price_id`),
  CONSTRAINT `fk_boarding_pass_ticket_passenger` FOREIGN KEY (`boarding_pass_passenger_id`) REFERENCES `boarding_pass_passenger` (`id`),
  CONSTRAINT `fk_boarding_pass_ticket_price` FOREIGN KEY (`config_ticket_price_id`) REFERENCES `config_ticket_price` (`id`),
  CONSTRAINT `fk_boarding_pass_ticket_route` FOREIGN KEY (`boarding_pass_route_id`) REFERENCES `boarding_pass_route` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `boarding_pass_ticket`
--

LOCK TABLES `boarding_pass_ticket` WRITE;
/*!40000 ALTER TABLE `boarding_pass_ticket` DISABLE KEYS */;
/*!40000 ALTER TABLE `boarding_pass_ticket` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `branchoffice`
--

DROP TABLE IF EXISTS `branchoffice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `branchoffice` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `prefix` varchar(10) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `description` text,
  `address` varchar(255) NOT NULL,
  `street_id` int(11) NOT NULL,
  `no_ext` varchar(15) DEFAULT NULL,
  `no_int` varchar(15) DEFAULT NULL,
  `suburb_id` int(11) NOT NULL,
  `city_id` int(11) NOT NULL,
  `county_id` int(11) DEFAULT NULL,
  `state_id` int(11) NOT NULL,
  `country_id` int(11) NOT NULL,
  `zip_code` int(11) DEFAULT NULL,
  `reference` text,
  `branch_office_type` enum('A','O','T','V') NOT NULL DEFAULT 'O',
  `latitude` text NOT NULL,
  `longitude` text NOT NULL,
  `manager_id` int(11) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `time_zone` varchar(254) DEFAULT NULL,
  `time_checkpoint` int(11) DEFAULT NULL,
  `time_manteinance` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `prefix` (`prefix`),
  KEY `manager_id` (`manager_id`),
  KEY `street_id` (`street_id`),
  KEY `suburb_id` (`suburb_id`),
  KEY `city_id` (`city_id`),
  KEY `state_id` (`state_id`),
  KEY `country_id` (`country_id`),
  KEY `fk_branchoffice_county` (`county_id`),
  CONSTRAINT `branchoffice_ibfk_1` FOREIGN KEY (`manager_id`) REFERENCES `employee` (`id`),
  CONSTRAINT `branchoffice_ibfk_2` FOREIGN KEY (`street_id`) REFERENCES `street` (`id`),
  CONSTRAINT `branchoffice_ibfk_3` FOREIGN KEY (`suburb_id`) REFERENCES `suburb` (`id`),
  CONSTRAINT `branchoffice_ibfk_4` FOREIGN KEY (`city_id`) REFERENCES `city` (`id`),
  CONSTRAINT `branchoffice_ibfk_5` FOREIGN KEY (`state_id`) REFERENCES `state` (`id`),
  CONSTRAINT `branchoffice_ibfk_6` FOREIGN KEY (`country_id`) REFERENCES `country` (`id`),
  CONSTRAINT `fk_branchoffice_county` FOREIGN KEY (`county_id`) REFERENCES `county` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `branchoffice`
--

LOCK TABLES `branchoffice` WRITE;
/*!40000 ALTER TABLE `branchoffice` DISABLE KEYS */;
/*!40000 ALTER TABLE `branchoffice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `branchoffice_schedule`
--

DROP TABLE IF EXISTS `branchoffice_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `branchoffice_schedule` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `branchoffice_id` int(11) NOT NULL,
  `hour_start` varchar(5) NOT NULL DEFAULT '00:00',
  `hour_end` varchar(5) NOT NULL DEFAULT '00:00',
  `break_start` varchar(5) DEFAULT '00:00',
  `break_end` varchar(5) DEFAULT '00:00',
  `date_start` date DEFAULT NULL,
  `date_end` date DEFAULT NULL,
  `notes` text,
  `sun` tinyint(1) NOT NULL DEFAULT '0',
  `mon` tinyint(1) NOT NULL DEFAULT '0',
  `thu` tinyint(1) NOT NULL DEFAULT '0',
  `wen` tinyint(1) NOT NULL DEFAULT '0',
  `tue` tinyint(1) NOT NULL DEFAULT '0',
  `fri` tinyint(1) NOT NULL DEFAULT '0',
  `sat` tinyint(1) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `branchoffice_id` (`branchoffice_id`),
  CONSTRAINT `branchoffice_schedule_ibfk_1` FOREIGN KEY (`branchoffice_id`) REFERENCES `branchoffice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `branchoffice_schedule`
--

LOCK TABLES `branchoffice_schedule` WRITE;
/*!40000 ALTER TABLE `branchoffice_schedule` DISABLE KEYS */;
/*!40000 ALTER TABLE `branchoffice_schedule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `casefile`
--

DROP TABLE IF EXISTS `casefile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `casefile` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `description` varchar(254) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `type_casefile` enum('Employee','Vehicle','Customer') DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `cash_out`
--

DROP TABLE IF EXISTS `cash_out`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cash_out` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `branchoffice_id` int(11) NOT NULL,
  `employee_id` int(11) NOT NULL,
  `token` varchar(8000) NOT NULL,
  `ip` char(15) NOT NULL,
  `initial_fund` decimal(12,2) NOT NULL,
  `final_fund` decimal(12,2) DEFAULT NULL,
  `cash` decimal(12,2) DEFAULT NULL,
  `vouchers` decimal(12,2) DEFAULT NULL,
  `total_reported` decimal(12,2) DEFAULT NULL,
  `total_on_register` decimal(12,2) DEFAULT NULL,
  `has_diference` enum('0','1','2') NOT NULL DEFAULT '0',
  `notes` varchar(8000) DEFAULT NULL,
  `cash_out_status` int(11) NOT NULL DEFAULT '1',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `cash_register_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_cash_out_branchoffice_id` (`branchoffice_id`),
  KEY `fk_cash_out_employee_id` (`employee_id`),
  KEY `fk_cash_out_cash_register_id` (`cash_register_id`),
  CONSTRAINT `fk_cash_out_branchoffice_id` FOREIGN KEY (`branchoffice_id`) REFERENCES `branchoffice` (`id`),
  CONSTRAINT `fk_cash_out_cash_register_id` FOREIGN KEY (`cash_register_id`) REFERENCES `cash_registers` (`id`),
  CONSTRAINT `fk_cash_out_employee_id` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cash_out`
--

LOCK TABLES `cash_out` WRITE;
/*!40000 ALTER TABLE `cash_out` DISABLE KEYS */;
/*!40000 ALTER TABLE `cash_out` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cash_out_detail`
--

DROP TABLE IF EXISTS `cash_out_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cash_out_detail` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cash_out_id` int(11) NOT NULL,
  `pieces` int(11) NOT NULL,
  `value` decimal(12,2) DEFAULT NULL,
  `accumulated` decimal(12,2) NOT NULL,
  `is_voucher` tinyint(1) NOT NULL DEFAULT '0',
  `currency_id` int(11) NOT NULL,
  `exchange_rate_id` int(11) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_cash_out_detail_cash_out_id` (`cash_out_id`),
  KEY `fk_cash_out_detail_currency_id` (`currency_id`),
  KEY `fk_cash_out_detail_exchange_rate_id` (`exchange_rate_id`),
  CONSTRAINT `fk_cash_out_detail_cash_out_id` FOREIGN KEY (`cash_out_id`) REFERENCES `cash_out` (`id`),
  CONSTRAINT `fk_cash_out_detail_currency_id` FOREIGN KEY (`currency_id`) REFERENCES `currency` (`id`),
  CONSTRAINT `fk_cash_out_detail_exchange_rate_id` FOREIGN KEY (`exchange_rate_id`) REFERENCES `exchange_rate` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cash_out_detail`
--

LOCK TABLES `cash_out_detail` WRITE;
/*!40000 ALTER TABLE `cash_out_detail` DISABLE KEYS */;
/*!40000 ALTER TABLE `cash_out_detail` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cash_out_move`
--

DROP TABLE IF EXISTS `cash_out_move`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cash_out_move` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cash_out_id` int(11) NOT NULL,
  `payment_id` int(11) NULL,
  `quantity` decimal(12,2) NOT NULL,
  `move_type` enum('0','1') NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_cash_out_move_payment_id` (`payment_id`),
  KEY `fk_cash_out_move_cash_out_id` (`cash_out_id`),
  CONSTRAINT `fk_cash_out_move_cash_out_id` FOREIGN KEY (`cash_out_id`) REFERENCES `cash_out` (`id`),
  CONSTRAINT `fk_cash_out_move_payment_id` FOREIGN KEY (`payment_id`) REFERENCES `payment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cash_out_move`
--

LOCK TABLES `cash_out_move` WRITE;
/*!40000 ALTER TABLE `cash_out_move` DISABLE KEYS */;
/*!40000 ALTER TABLE `cash_out_move` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cash_registers`
--

DROP TABLE IF EXISTS `cash_registers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cash_registers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `branchoffice_id` int(11) NOT NULL,
  `cash_register` varchar(60) DEFAULT NULL,
  `cash_out_status` tinyint(1) DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_cash_registers_branchoffice_id` (`branchoffice_id`),
  CONSTRAINT `fk_cash_registers_branchoffice_id` FOREIGN KEY (`branchoffice_id`) REFERENCES `branchoffice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cash_registers`
--

LOCK TABLES `cash_registers` WRITE;
/*!40000 ALTER TABLE `cash_registers` DISABLE KEYS */;
/*!40000 ALTER TABLE `cash_registers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `characteristic`
--

DROP TABLE IF EXISTS `characteristic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `characteristic` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `icon` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `city`
--

DROP TABLE IF EXISTS `city`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `city` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `county_id` int(11) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `municipality_id` (`county_id`),
  CONSTRAINT `city_ibfk_1` FOREIGN KEY (`county_id`) REFERENCES `county` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=692 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `complement`
--

DROP TABLE IF EXISTS `complement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `complement` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `description` varchar(100) DEFAULT NULL,
  `type_complement` enum('piece','size','package') NOT NULL,
  `travel_insurance` tinyint(1) NOT NULL,
  `travel_insurance_cost` decimal(12,2) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `complement_rule`
--

DROP TABLE IF EXISTS `complement_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `complement_rule` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `complement_id` int(11) NOT NULL,
  `min_quantity` int(11) NOT NULL,
  `max_quantity` int(11) NOT NULL,
  `max_weight` decimal(12,2) NOT NULL,
  `max_linear_volume` decimal(12,2) NOT NULL,
  `parent_id` int(11) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_complement_rule_complement_idx` (`complement_id`),
  KEY `fk_complement_rule_parent_idx` (`parent_id`),
  CONSTRAINT `fk_complement_rule_parent` FOREIGN KEY (`parent_id`) REFERENCES `complement_rule` (`id`),
  CONSTRAINT `fk_complement_rules_complement` FOREIGN KEY (`complement_id`) REFERENCES `complement` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `config_destination`
--

DROP TABLE IF EXISTS `config_destination`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `config_destination` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `config_route_id` int(11) NOT NULL,
  `terminal_origin_id` int(11) NOT NULL,
  `terminal_destiny_id` int(11) NOT NULL,
  `travel_time` varchar(5) NOT NULL DEFAULT '00:00',
  `order_route` int(11) NOT NULL DEFAULT '0',
  `distance_km` decimal(12,2) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `order_origin` int(11) DEFAULT '0',
  `order_destiny` int(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `config_route_id` (`config_route_id`),
  KEY `fk_conf_destination_terminal_destiny` (`terminal_destiny_id`),
  KEY `fk_conf_destination_terminal_origin` (`terminal_origin_id`),
  CONSTRAINT `config_destination_ibfk_1` FOREIGN KEY (`config_route_id`) REFERENCES `config_route` (`id`),
  CONSTRAINT `fk_conf_destination_terminal_destiny` FOREIGN KEY (`terminal_destiny_id`) REFERENCES `branchoffice` (`id`),
  CONSTRAINT `fk_conf_destination_terminal_origin` FOREIGN KEY (`terminal_origin_id`) REFERENCES `branchoffice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `config_destination`
--

LOCK TABLES `config_destination` WRITE;
/*!40000 ALTER TABLE `config_destination` DISABLE KEYS */;
/*!40000 ALTER TABLE `config_destination` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `config_price_route`
--

DROP TABLE IF EXISTS `config_price_route`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `config_price_route` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `config_route_id` int(11) NOT NULL,
  `total_kms` int(11) DEFAULT NULL,
  `cost_per_km` decimal(12,2) NOT NULL DEFAULT '0.00',
  `profiltableness` decimal(6,2) NOT NULL DEFAULT '0.00',
  `discount_id` int(11) DEFAULT NULL,
  `require_code` tinyint(1) NOT NULL DEFAULT '0',
  `price_status` int(1) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `config_route_id` (`config_route_id`),
  CONSTRAINT `config_price_route_ibfk_1` FOREIGN KEY (`config_route_id`) REFERENCES `config_route` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `config_price_route`
--

LOCK TABLES `config_price_route` WRITE;
/*!40000 ALTER TABLE `config_price_route` DISABLE KEYS */;
/*!40000 ALTER TABLE `config_price_route` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `config_route`
--

DROP TABLE IF EXISTS `config_route`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `config_route` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(30) NOT NULL,
  `description` text,
  `terminal_origin_id` int(11) NOT NULL,
  `terminal_destiny_id` int(11) NOT NULL,
  `travel_time` varchar(5) NOT NULL DEFAULT '00:00',
  `type_travel` enum('0','1','2') NOT NULL DEFAULT '0',
  `one_way` tinyint(1) NOT NULL DEFAULT '1',
  `from_id` int(11) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `discount_tickets` tinyint(1) NOT NULL DEFAULT '0',
  `expire_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_config_route_terminal_origin` (`terminal_origin_id`),
  KEY `fk_config_route_terminal_destiny` (`terminal_destiny_id`),
  CONSTRAINT `fk_config_route_terminal_destiny` FOREIGN KEY (`terminal_destiny_id`) REFERENCES `branchoffice` (`id`),
  CONSTRAINT `fk_config_route_terminal_origin` FOREIGN KEY (`terminal_origin_id`) REFERENCES `branchoffice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `config_route`
--

LOCK TABLES `config_route` WRITE;
/*!40000 ALTER TABLE `config_route` DISABLE KEYS */;
/*!40000 ALTER TABLE `config_route` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `config_schedule`
--

DROP TABLE IF EXISTS `config_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `config_schedule` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `config_route_id` int(11) NOT NULL,
  `terminal_origin_id` int(11) NOT NULL,
  `travel_hour` varchar(5) NOT NULL DEFAULT '00:00',
  `sun` tinyint(1) NOT NULL DEFAULT '0',
  `mon` tinyint(1) NOT NULL DEFAULT '0',
  `thu` tinyint(1) NOT NULL DEFAULT '0',
  `wen` tinyint(1) NOT NULL DEFAULT '0',
  `tue` tinyint(1) NOT NULL DEFAULT '0',
  `fri` tinyint(1) NOT NULL DEFAULT '0',
  `sat` tinyint(1) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `config_schedule_origin_id` int(11) DEFAULT NULL,
  `from_id` int(11) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_confSchedule_terminal_orifin` (`terminal_origin_id`),
  KEY `fk_config_schedule_config_route` (`config_route_id`),
  KEY `fk_config_schedule_origin_id_idx` (`config_schedule_origin_id`),
  KEY `fk_config_schedule_from_id_idx` (`from_id`),
  CONSTRAINT `fk_confSchedule_terminal_orifin` FOREIGN KEY (`terminal_origin_id`) REFERENCES `branchoffice` (`id`),
  CONSTRAINT `fk_config_schedule_config_route` FOREIGN KEY (`config_route_id`) REFERENCES `config_route` (`id`),
  CONSTRAINT `fk_config_schedule_from_id` FOREIGN KEY (`from_id`) REFERENCES `config_schedule` (`id`),
  CONSTRAINT `fk_config_schedule_origin_id` FOREIGN KEY (`config_schedule_origin_id`) REFERENCES `config_schedule` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `config_schedule`
--

LOCK TABLES `config_schedule` WRITE;
/*!40000 ALTER TABLE `config_schedule` DISABLE KEYS */;
/*!40000 ALTER TABLE `config_schedule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `config_system`
--

DROP TABLE IF EXISTS `config_system`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `config_system` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cron_exchange_rate_update_active` tinyint(1) NOT NULL DEFAULT '1',
  `cron_exchange_rate_abordo_minus_qty` decimal(12,2) NOT NULL DEFAULT '1.00',
  `cron_exchange_rate_updated_last_time` date DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;



--
-- Table structure for table `config_ticket_price`
--

DROP TABLE IF EXISTS `config_ticket_price`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `config_ticket_price` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `config_prices_route_id` int(11) DEFAULT NULL,
  `special_ticket_id` int(11) NOT NULL,
  `amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `discount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `price_status` tinyint(1) NOT NULL DEFAULT '1',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `config_destination_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_special_ticket_destination` (`special_ticket_id`,`config_destination_id`),
  KEY `config_prices_route_id` (`config_prices_route_id`),
  KEY `special_ticket_id` (`special_ticket_id`),
  KEY `fk_config_ticket_price_config_destination` (`config_destination_id`),
  CONSTRAINT `config_ticket_price_ibfk_1` FOREIGN KEY (`config_prices_route_id`) REFERENCES `config_price_route` (`id`),
  CONSTRAINT `config_ticket_price_ibfk_2` FOREIGN KEY (`special_ticket_id`) REFERENCES `special_ticket` (`id`),
  CONSTRAINT `fk_config_ticket_price_config_destination` FOREIGN KEY (`config_destination_id`) REFERENCES `config_destination` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `config_ticket_price`
--

LOCK TABLES `config_ticket_price` WRITE;
/*!40000 ALTER TABLE `config_ticket_price` DISABLE KEYS */;
/*!40000 ALTER TABLE `config_ticket_price` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `config_vehicle`
--

DROP TABLE IF EXISTS `config_vehicle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `config_vehicle` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `seatings` int(11) NOT NULL DEFAULT '1',
  `total_row` int(11) NOT NULL DEFAULT '1',
  `total_col` int(11) NOT NULL DEFAULT '1',
  `seat_by_row` int(11) NOT NULL DEFAULT '1',
  `seat_by_col` int(11) NOT NULL DEFAULT '1',
  `division_by_row` varchar(20) DEFAULT NULL,
  `division_by_col` varchar(20) DEFAULT NULL,
  `enumeration` enum('0','1') NOT NULL DEFAULT '0',
  `enum_type` enum('0','1','2') DEFAULT NULL,
  `not_seats` varchar(100) DEFAULT NULL,
  `emergency_exit` varchar(30) DEFAULT NULL,
  `total_special_seats_handicapped` tinyint(4) DEFAULT NULL,
  `special_seats_handicapped` varchar(100) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `name` varchar(50) NOT NULL,
  `base` tinyint(1) NOT NULL DEFAULT '0',
  `is_base` tinyint(1) NOT NULL DEFAULT '0',
  `total_special_seats_women` tinyint(4) DEFAULT NULL,
  `special_seats_women` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `country`
--

DROP TABLE IF EXISTS `country`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `country` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `phone_code` varchar(3) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;



--
-- Table structure for table `county`
--

DROP TABLE IF EXISTS `county`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `county` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `state_id` int(11) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `state_id` (`state_id`),
  CONSTRAINT `county_ibfk_1` FOREIGN KEY (`state_id`) REFERENCES `state` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2323 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `currency`
--

DROP TABLE IF EXISTS `currency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `currency` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `abr` varchar(5) NOT NULL,
  `symbol` varchar(30) NOT NULL,
  `icon` varchar(30) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `currency_denomination`
--

DROP TABLE IF EXISTS `currency_denomination`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `currency_denomination` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `currency_id` int(11) NOT NULL,
  `denomination` decimal(12,2) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_currency_denomination_currency_id` (`currency_id`),
  CONSTRAINT `fk_currency_denomination_currency_id` FOREIGN KEY (`currency_id`) REFERENCES `currency` (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `customer`
--

DROP TABLE IF EXISTS `customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `first_name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `gender` enum('m','f','n') NOT NULL,
  `image` text,
  `phone` varchar(13) NOT NULL,
  `email` varchar(100) NOT NULL,
  `birthday` date DEFAULT NULL,
  `token` text,
  `is_business` tinyint(1) NOT NULL DEFAULT '0',
  `times_complain` int(11) DEFAULT NULL,
  `times_blocked` int(11) DEFAULT NULL,
  `registered_at` datetime DEFAULT NULL,
  `conekta_id` varchar(25) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `has_credit` tinyint(1) NOT NULL DEFAULT '0',
  `credit_limit` decimal(12,2) DEFAULT NULL,
  `credit_time_limit` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer`
--

LOCK TABLES `customer` WRITE;
/*!40000 ALTER TABLE `customer` DISABLE KEYS */;
/*!40000 ALTER TABLE `customer` ENABLE KEYS */;
UNLOCK TABLES;


--
-- Table structure for table `customer_billing_information`
--

DROP TABLE IF EXISTS `customer_billing_information`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_billing_information` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `customer_id` int(11) NOT NULL,
  `name` varchar(50) NOT NULL,
  `rfc` varchar(13) NOT NULL,
  `address` varchar(255) NOT NULL,
  `street_id` int(11) NOT NULL,
  `no_ext` varchar(15) DEFAULT NULL,
  `no_int` varchar(15) DEFAULT NULL,
  `suburb_id` int(11) NOT NULL,
  `city_id` int(11) NOT NULL,
  `county_id` int(11) DEFAULT NULL,
  `state_id` int(11) NOT NULL,
  `country_id` int(11) NOT NULL,
  `reference` text,
  `zip_code` int(11) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `legal_person` enum('fisico','moral') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `rfc` (`rfc`),
  KEY `customer_billing_info_customer` (`customer_id`),
  KEY `customer_billing_info_street` (`street_id`),
  KEY `customer_billing_info_suburb` (`suburb_id`),
  KEY `customer_billing_info_city` (`city_id`),
  KEY `customer_billing_info_state` (`state_id`),
  KEY `customer_billing_info_country` (`country_id`),
  KEY `customer_billing_info_county` (`county_id`),
  CONSTRAINT `customer_billing_info_city` FOREIGN KEY (`city_id`) REFERENCES `city` (`id`),
  CONSTRAINT `customer_billing_info_country` FOREIGN KEY (`country_id`) REFERENCES `country` (`id`),
  CONSTRAINT `customer_billing_info_county` FOREIGN KEY (`county_id`) REFERENCES `county` (`id`),
  CONSTRAINT `customer_billing_info_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`),
  CONSTRAINT `customer_billing_info_state` FOREIGN KEY (`state_id`) REFERENCES `state` (`id`),
  CONSTRAINT `customer_billing_info_street` FOREIGN KEY (`street_id`) REFERENCES `street` (`id`),
  CONSTRAINT `customer_billing_info_suburb` FOREIGN KEY (`suburb_id`) REFERENCES `suburb` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer_billing_information`
--

LOCK TABLES `customer_billing_information` WRITE;
/*!40000 ALTER TABLE `customer_billing_information` DISABLE KEYS */;
/*!40000 ALTER TABLE `customer_billing_information` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customer_casefile`
--

DROP TABLE IF EXISTS `customer_casefile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_casefile` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `customer_id` int(11) NOT NULL,
  `casefile_id` int(11) NOT NULL,
  `file` text,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_customer_id_casefile` (`customer_id`),
  KEY `fk_casefile_id_customer_casefile` (`casefile_id`),
  CONSTRAINT `fk_casefile_id_customer_casefile` FOREIGN KEY (`casefile_id`) REFERENCES `casefile` (`id`),
  CONSTRAINT `fk_customer_id_casefile` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer_casefile`
--

LOCK TABLES `customer_casefile` WRITE;
/*!40000 ALTER TABLE `customer_casefile` DISABLE KEYS */;
/*!40000 ALTER TABLE `customer_casefile` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customers_personalauth`
--

DROP TABLE IF EXISTS `customers_personalauth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customers_personalauth` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `customer_id` int(11) NOT NULL,
  `first_name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `phone` varchar(13) NOT NULL DEFAULT '',
  `email` varchar(100) NOT NULL DEFAULT '',
  `notes` text,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `customer_personalauth_customer_id_fk` (`customer_id`),
  CONSTRAINT `customer_personalauth_customer_id_fk` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customers_personalauth`
--

LOCK TABLES `customers_personalauth` WRITE;
/*!40000 ALTER TABLE `customers_personalauth` DISABLE KEYS */;
/*!40000 ALTER TABLE `customers_personalauth` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `driver_tracking`
--

DROP TABLE IF EXISTS `driver_tracking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `driver_tracking` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `employee_id` int(11) NOT NULL,
  `time_tracking` char(5) DEFAULT NULL,
  `location_started` varchar(255) NOT NULL,
  `location_finished` varchar(255) DEFAULT NULL,
  `was_completed` tinyint(1) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_driver_tracking_employee` (`employee_id`),
  CONSTRAINT `fk_driver_tracking_employee` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `driver_tracking`
--

LOCK TABLES `driver_tracking` WRITE;
/*!40000 ALTER TABLE `driver_tracking` DISABLE KEYS */;
/*!40000 ALTER TABLE `driver_tracking` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employee`
--

DROP TABLE IF EXISTS `employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `employee` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `birthday` date NOT NULL,
  `cellphone` varchar(10) NOT NULL,
  `phone` varchar(10) DEFAULT NULL,
  `email` varchar(100) NOT NULL,
  `address` varchar(255) NOT NULL,
  `street_id` int(11) NOT NULL,
  `no_ext` varchar(15) DEFAULT NULL,
  `no_int` varchar(15) DEFAULT NULL,
  `suburb_id` int(11) NOT NULL,
  `city_id` int(11) NOT NULL,
  `county_id` int(11) DEFAULT NULL,
  `state_id` int(11) NOT NULL,
  `country_id` int(11) NOT NULL,
  `reference` text,
  `marital_status` enum('single','married','widowed') DEFAULT NULL,
  `branchoffice_id` int(11) NOT NULL,
  `job_id` int(11) NOT NULL,
  `paysheet_type_id` int(11) NOT NULL,
  `salary` decimal(10,2) NOT NULL DEFAULT '0.00',
  `is_fulltime` tinyint(1) NOT NULL DEFAULT '1',
  `skills` text,
  `rfc` varchar(13) DEFAULT NULL,
  `curp` varchar(25) DEFAULT NULL,
  `nss` varchar(255) DEFAULT NULL,
  `blood_type` tinyint(4) DEFAULT NULL,
  `health_notes` text,
  `alergies` text,
  `start_working_at` date DEFAULT NULL,
  `finish_working_at` date DEFAULT NULL,
  `notes` text,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `zip_code` int(11) DEFAULT NULL,
  `gender` enum('m','f','n') DEFAULT NULL,
  `scholarship` enum('elementary','middleschool','technical','highschool','college','master','phd','other') NOT NULL,
  `avatar_file` text,
  `boss_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `rfc` (`rfc`),
  UNIQUE KEY `curp` (`curp`),
  KEY `street_id` (`street_id`),
  KEY `suburb_id` (`suburb_id`),
  KEY `city_id` (`city_id`),
  KEY `state_id` (`state_id`),
  KEY `country_id` (`country_id`),
  KEY `paysheet_type_id` (`paysheet_type_id`),
  KEY `job_id` (`job_id`),
  KEY `fk_employee_branchoffice` (`branchoffice_id`),
  KEY `fk_employee_county` (`county_id`),
  KEY `fk_employee_boss` (`boss_id`),
  CONSTRAINT `employee_ibfk_1` FOREIGN KEY (`street_id`) REFERENCES `street` (`id`),
  CONSTRAINT `employee_ibfk_2` FOREIGN KEY (`suburb_id`) REFERENCES `suburb` (`id`),
  CONSTRAINT `employee_ibfk_3` FOREIGN KEY (`city_id`) REFERENCES `city` (`id`),
  CONSTRAINT `employee_ibfk_4` FOREIGN KEY (`state_id`) REFERENCES `state` (`id`),
  CONSTRAINT `employee_ibfk_5` FOREIGN KEY (`country_id`) REFERENCES `country` (`id`),
  CONSTRAINT `employee_ibfk_6` FOREIGN KEY (`paysheet_type_id`) REFERENCES `paysheet_type` (`id`),
  CONSTRAINT `employee_ibfk_7` FOREIGN KEY (`job_id`) REFERENCES `job` (`id`),
  CONSTRAINT `fk_employee_boss` FOREIGN KEY (`boss_id`) REFERENCES `employee` (`id`),
  CONSTRAINT `fk_employee_branchoffice` FOREIGN KEY (`branchoffice_id`) REFERENCES `branchoffice` (`id`),
  CONSTRAINT `fk_employee_county` FOREIGN KEY (`county_id`) REFERENCES `county` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employee`
--

LOCK TABLES `employee` WRITE;
/*!40000 ALTER TABLE `employee` DISABLE KEYS */;
/*!40000 ALTER TABLE `employee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employee_casefile`
--

DROP TABLE IF EXISTS `employee_casefile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `employee_casefile` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `employee_id` int(11) NOT NULL,
  `casefile_id` int(11) NOT NULL,
  `file` text,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `employee_id` (`employee_id`),
  KEY `casefile_id` (`casefile_id`),
  CONSTRAINT `employee_casefile_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`),
  CONSTRAINT `employee_casefile_ibfk_2` FOREIGN KEY (`casefile_id`) REFERENCES `casefile` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employee_casefile`
--

LOCK TABLES `employee_casefile` WRITE;
/*!40000 ALTER TABLE `employee_casefile` DISABLE KEYS */;
/*!40000 ALTER TABLE `employee_casefile` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employee_contact`
--

DROP TABLE IF EXISTS `employee_contact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `employee_contact` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `employee_id` int(11) NOT NULL,
  `name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `phone` varchar(10) NOT NULL,
  `relationship` int(11) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `employee_id` (`employee_id`),
  CONSTRAINT `employee_contact_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employee_contact`
--

LOCK TABLES `employee_contact` WRITE;
/*!40000 ALTER TABLE `employee_contact` DISABLE KEYS */;
/*!40000 ALTER TABLE `employee_contact` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employee_requirement`
--

DROP TABLE IF EXISTS `employee_requirement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `employee_requirement` (
  `employee_id` int(11) NOT NULL,
  `req_value` text,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `requirement_id` int(11) NOT NULL,
  `job_id` int(11) NOT NULL,
  `req_status` int(11) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `employee_id` (`employee_id`),
  KEY `fk_employee_requirement_req_id` (`requirement_id`),
  KEY `fk_employee_requirement_job_id` (`job_id`),
  CONSTRAINT `employee_requirement_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`),
  CONSTRAINT `fk_employee_requirement_job_id` FOREIGN KEY (`job_id`) REFERENCES `job` (`id`),
  CONSTRAINT `fk_employee_requirement_req_id` FOREIGN KEY (`requirement_id`) REFERENCES `requirement` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employee_requirement`
--

LOCK TABLES `employee_requirement` WRITE;
/*!40000 ALTER TABLE `employee_requirement` DISABLE KEYS */;
/*!40000 ALTER TABLE `employee_requirement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employee_schedule`
--

DROP TABLE IF EXISTS `employee_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `employee_schedule` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `employee_id` int(11) NOT NULL,
  `hour_start` varchar(5) NOT NULL DEFAULT '00:00',
  `hour_end` varchar(5) NOT NULL DEFAULT '00:00',
  `break_start` varchar(5) DEFAULT '00:00',
  `break_end` varchar(5) DEFAULT '00:00',
  `date_start` date NOT NULL,
  `date_end` date DEFAULT NULL,
  `notes` text,
  `sun` tinyint(1) NOT NULL DEFAULT '0',
  `mon` tinyint(1) NOT NULL DEFAULT '0',
  `thu` tinyint(1) NOT NULL DEFAULT '0',
  `wen` tinyint(1) NOT NULL DEFAULT '0',
  `tue` tinyint(1) NOT NULL DEFAULT '0',
  `fri` tinyint(1) NOT NULL DEFAULT '0',
  `sat` tinyint(1) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `employee_id` (`employee_id`),
  CONSTRAINT `employee_schedule_ibfk_1` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employee_schedule`
--

LOCK TABLES `employee_schedule` WRITE;
/*!40000 ALTER TABLE `employee_schedule` DISABLE KEYS */;
/*!40000 ALTER TABLE `employee_schedule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exchange_rate`
--

DROP TABLE IF EXISTS `exchange_rate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `exchange_rate` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `base_currency` int(11) NOT NULL,
  `currency_id` int(11) NOT NULL,
  `change_amount` decimal(16,6) NOT NULL,
  `data_source` tinyint(4) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `serie_id` varchar(7) DEFAULT NULL,
  `abordo_rate` decimal(12,2) NOT NULL,
  `date_update` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `currency_id` (`currency_id`),
  KEY `base_currency` (`base_currency`),
  CONSTRAINT `exchange_rate_ibfk_1` FOREIGN KEY (`currency_id`) REFERENCES `currency` (`id`),
  CONSTRAINT `exchange_rate_ibfk_2` FOREIGN KEY (`base_currency`) REFERENCES `currency` (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

DROP TABLE IF EXISTS `expense`;
CREATE TABLE `expense` (
  `id` int(11) PRIMARY key NOT NULL AUTO_INCREMENT,
  `boarding_pass_id` int(11),
  `rental_id` int(11),
  `parcel_id` int(11),
  `expense_concept_id` int(11),
  `payment_method_id` int not NULL,
  `amount` decimal(12,2) NOT NULL DEFAULT '1.00',
  `reference` varchar(254),
  `exchange_rate_id` int(11),
  `currency_id` int(11),
  `description` varchar(254),
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11),
  `updated_at` datetime,
  `updated_by` int(11),
  CONSTRAINT `fk_expense_boarding_pass_id` FOREIGN KEY (`boarding_pass_id`) REFERENCES `boarding_pass` (`id`),
  CONSTRAINT `fk_expense_currency_id` FOREIGN KEY (`currency_id`) REFERENCES `currency` (`id`),
  CONSTRAINT `fk_expense_exchange_rate_id` FOREIGN KEY (`exchange_rate_id`) REFERENCES `exchange_rate` (`id`),
  CONSTRAINT `fk_expense_payment_method_id` FOREIGN KEY (`payment_method_id`) REFERENCES `payment_method` (`id`),
  CONSTRAINT `fk_expense_rental_id` FOREIGN KEY (`rental_id`) REFERENCES `rental` (`id`)
);

--
-- Table structure for table `expense_concept`
--

DROP TABLE IF EXISTS `expense_concept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `expense_concept` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` text,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;



--
-- Table structure for table `expense_schedule`
--

DROP TABLE IF EXISTS `expense_schedule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `expense_schedule` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `branchoffice_id` int(11) NOT NULL,
  `expense_concept_id` int(11) NOT NULL,
  `general_concept` tinyint(4) NOT NULL DEFAULT '0',
  `period` enum('1','2','3') DEFAULT NULL,
  `quantity` int(11) DEFAULT NULL,
  `last_time` date DEFAULT NULL,
  `next_time` date DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `branch_office_id` (`branchoffice_id`),
  KEY `expense_concept_id` (`expense_concept_id`),
  CONSTRAINT `expense_schedule_ibfk_1` FOREIGN KEY (`branchoffice_id`) REFERENCES `branchoffice` (`id`),
  CONSTRAINT `expense_schedule_ibfk_2` FOREIGN KEY (`expense_concept_id`) REFERENCES `expense_concept` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `expense_schedule`
--

LOCK TABLES `expense_schedule` WRITE;
/*!40000 ALTER TABLE `expense_schedule` DISABLE KEYS */;
/*!40000 ALTER TABLE `expense_schedule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `extra_charge`
--

DROP TABLE IF EXISTS `extra_charge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `extra_charge` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `description` varchar(254) NOT NULL,
  `required_reference` tinyint(1) NOT NULL DEFAULT '0',
  `on_reception` tinyint(1) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `on_driver` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `general_setting`
--

DROP TABLE IF EXISTS `general_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `general_setting` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `FIELD` varchar(255) DEFAULT NULL,
  `value` text,
  `type_field` varchar(255) DEFAULT NULL,
  `required_field` tinyint(1) NOT NULL DEFAULT '0',
  `description` text,
  `value_default` text,
  `label_text` text,
  `explanation_text` text,
  `order_field` int(11) DEFAULT NULL,
  `type_setting` enum('0','1','2') DEFAULT NULL,
  `group_type` varchar(255) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `incidences`
--

DROP TABLE IF EXISTS `incidences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `incidences` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `description` varchar(254) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `incidences`
--

LOCK TABLES `incidences` WRITE;
/*!40000 ALTER TABLE `incidences` DISABLE KEYS */;
/*!40000 ALTER TABLE `incidences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `job`
--

DROP TABLE IF EXISTS `job`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `description` varchar(254) NOT NULL,
  `file_description` varchar(254) DEFAULT NULL,
  `salary` decimal(10,2) NOT NULL DEFAULT '0.00',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `job_casefile`
--

DROP TABLE IF EXISTS `job_casefile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_casefile` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `job_id` int(11) NOT NULL,
  `casefile_id` int(11) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_job_id_casefile` (`job_id`),
  KEY `fk_casefile_id_customer_ob` (`casefile_id`),
  CONSTRAINT `fk_casefile_id_customer_ob` FOREIGN KEY (`casefile_id`) REFERENCES `casefile` (`id`),
  CONSTRAINT `fk_job_id_casefile` FOREIGN KEY (`job_id`) REFERENCES `job` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `job_casefile`
--

LOCK TABLES `job_casefile` WRITE;
/*!40000 ALTER TABLE `job_casefile` DISABLE KEYS */;
/*!40000 ALTER TABLE `job_casefile` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `job_function`
--

DROP TABLE IF EXISTS `job_function`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_function` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` text,
  `job_id` int(11) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_job_id_job_function` (`job_id`),
  CONSTRAINT `fk_job_id_job_function` FOREIGN KEY (`job_id`) REFERENCES `job` (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `job_requirement`
--

DROP TABLE IF EXISTS `job_requirement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `job_requirement` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `job_id` int(11) NOT NULL,
  `requirement_id` int(11) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_job_id_requirement` (`job_id`),
  KEY `fk_job_id_requirement_2` (`requirement_id`),
  CONSTRAINT `fk_job_id_requirement` FOREIGN KEY (`job_id`) REFERENCES `job` (`id`),
  CONSTRAINT `fk_job_id_requirement_2` FOREIGN KEY (`requirement_id`) REFERENCES `requirement` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `job_requirement`
--

LOCK TABLES `job_requirement` WRITE;
/*!40000 ALTER TABLE `job_requirement` DISABLE KEYS */;
/*!40000 ALTER TABLE `job_requirement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mail_addressee`
--

DROP TABLE IF EXISTS `mail_addressee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mail_addressee` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `mail_type` varchar(100) NOT NULL,
  `description` text NOT NULL,
  `group_type` enum('exchange_rates','vehicles','employees','routes','travels','packages','rentals') NOT NULL,
  `job_id` int(11) DEFAULT NULL,
  `profile_id` int(11) DEFAULT NULL,
  `by_city` tinyint(1) NOT NULL DEFAULT '0',
  `by_branchoffice` tinyint(1) NOT NULL DEFAULT '0',
  `copy_owner` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_job_id` (`job_id`),
  CONSTRAINT `fk_job_id` FOREIGN KEY (`job_id`) REFERENCES `job` (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `package_price`
--

DROP TABLE IF EXISTS `package_price`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `package_price` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name_price` varchar(45) NOT NULL,
  `min_linear_volume` decimal(12,2) NOT NULL,
  `max_linear_volume` decimal(12,2) NOT NULL,
  `min_weight` decimal(12,2) NOT NULL,
  `max_weight` decimal(12,2) NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `currency_id` int(11) NOT NULL,
  `parent_id` int(11) DEFAULT NULL,
  `shipping_type` enum('parcel','courier') NOT NULL DEFAULT 'parcel',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_ by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_package_price_curreny_idx` (`currency_id`),
  KEY `fk_package_price_parent_idx` (`parent_id`),
  CONSTRAINT `fk_package_price_curreny` FOREIGN KEY (`currency_id`) REFERENCES `currency` (`id`),
  CONSTRAINT `fk_package_price_parent` FOREIGN KEY (`parent_id`) REFERENCES `package_price` (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `package_price_km`
--

DROP TABLE IF EXISTS `package_price_km`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `package_price_km` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `min_km` decimal(12,2) NOT NULL,
  `max_km` decimal(12,2) NOT NULL,
  `price` decimal(12,2) NOT NULL,
  `currency_id` int(11) DEFAULT NULL,
  `parent_id` int(11) DEFAULT NULL,
  `shipping_type` enum('parcel','courier') NOT NULL DEFAULT 'parcel',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_ by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_package_price_km_currency_idx` (`currency_id`),
  KEY `fk_package_price_km_parent_idx` (`parent_id`),
  CONSTRAINT `fk_package_price_km_currency` FOREIGN KEY (`currency_id`) REFERENCES `currency` (`id`),
  CONSTRAINT `fk_package_price_km_parent` FOREIGN KEY (`parent_id`) REFERENCES `package_price_km` (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `packings`
--

DROP TABLE IF EXISTS `packings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `packings` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `description` varchar(254) NOT NULL,
  `volume_min` int(11) DEFAULT NULL,
  `volume_max` int(11) DEFAULT NULL,
  `weight_min` decimal(10,4) DEFAULT NULL,
  `weight_max` decimal(10,4) DEFAULT NULL,
  `height` decimal(12,2) DEFAULT NULL,
  `width` decimal(12,2) DEFAULT NULL,
  `lenght` decimal(12,2) DEFAULT NULL,
  `cost` decimal(12,2) DEFAULT NULL,
  `currency_id` decimal(12,2) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `currency_id` (`currency_id`),
  CONSTRAINT `packings_fk_1` FOREIGN KEY (`id`) REFERENCES `currency` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `packings`
--

LOCK TABLES `packings` WRITE;
/*!40000 ALTER TABLE `packings` DISABLE KEYS */;
/*!40000 ALTER TABLE `packings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parcels`
--

DROP TABLE IF EXISTS `parcels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `parcels` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `customer_id` int(11) DEFAULT NULL,
  `is_customer` tinyint(1) NOT NULL DEFAULT '0',
  `total_packages` int(11) NOT NULL,
  `delivery_time` int(11) NOT NULL,
  `sender_id` int(11) DEFAULT NULL,
  `sender_name` varchar(50) NOT NULL,
  `sender_last_name` varchar(50) NOT NULL,
  `sender_phone` varchar(13) NOT NULL,
  `sender_email` varchar(100) NOT NULL DEFAULT '',
  `sender_zip_code` int(11) DEFAULT NULL,
  `sender_address` varchar(254) NOT NULL DEFAULT '',
  `terminal_origin_id` int(11) NOT NULL,
  `terminal_destiny_id` int(11) NOT NULL,
  `has_invoice` tinyint(1) NOT NULL DEFAULT '0',
  `num_invoice` varchar(20) DEFAULT NULL,
  `exchange_rate_id` int(11) DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `discount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `discount_code_id` int(11) DEFAULT NULL,
  `has_insurance` tinyint(1) NOT NULL DEFAULT '0',
  `insurance_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `extra_charges` decimal(12,2) NOT NULL DEFAULT '0.00',
  `iva` decimal(12,2) DEFAULT '0.00',
  `total_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `has_multiple_addressee` tinyint(1) NOT NULL DEFAULT '0',
  `addressee_id` int(11) DEFAULT NULL,
  `addressee_name` varchar(50) NOT NULL,
  `addressee_last_name` varchar(50) NOT NULL,
  `addressee_phone` varchar(13) NOT NULL,
  `addressee_email` varchar(100) NOT NULL DEFAULT '',
  `addressee_zip_code` int(11) DEFAULT NULL,
  `addressee_address` varchar(254) NOT NULL DEFAULT '',
  `parcel_tracking_code` varchar(60) DEFAULT NULL,
  `pays_sender` tinyint(1) NOT NULL DEFAULT '1',
  `parcel_status` int(11) NOT NULL DEFAULT '1',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `parcels_customer_fk` (`customer_id`),
  KEY `parcels_terminal_origin_id_fk` (`terminal_origin_id`),
  KEY `parcels_exchange_rate_id_fk` (`exchange_rate_id`),
  KEY `parcels_terminal_destiy_id_fk` (`terminal_destiny_id`),
  KEY `parcels_sender_id_fk` (`sender_id`),
  KEY `parcels_addressee_id_fk` (`addressee_id`),
  CONSTRAINT `parcels_addressee_id_fk` FOREIGN KEY (`addressee_id`) REFERENCES `customer` (`id`),
  CONSTRAINT `parcels_customer_fk` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`),
  CONSTRAINT `parcels_exchange_rate_id` FOREIGN KEY (`exchange_rate_id`) REFERENCES `exchange_rate` (`id`),
  CONSTRAINT `parcels_sender_id` FOREIGN KEY (`sender_id`) REFERENCES `customer` (`id`),
  CONSTRAINT `parcels_terminal_destiy_id` FOREIGN KEY (`terminal_destiny_id`) REFERENCES `branchoffice` (`id`),
  CONSTRAINT `parcels_terminal_origin_id_fk` FOREIGN KEY (`terminal_origin_id`) REFERENCES `branchoffice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parcels`
--

LOCK TABLES `parcels` WRITE;
/*!40000 ALTER TABLE `parcels` DISABLE KEYS */;
/*!40000 ALTER TABLE `parcels` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parcels_allowed`
--

DROP TABLE IF EXISTS `parcels_allowed`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `parcels_allowed` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `description` varchar(254) NOT NULL,
  `has_insurance` tinyint(1) NOT NULL DEFAULT '0',
  `insurance_max_amount` decimal(12,2) DEFAULT NULL,
  `cost_per_range` decimal(12,2) NOT NULL DEFAULT '0.00',
  `cost_insurance_range` decimal(12,2) NOT NULL DEFAULT '0.00',
  `need_auth` tinyint(1) NOT NULL DEFAULT '0',
  `need_documentation` tinyint(1) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parcels_allowed`
--

LOCK TABLES `parcels_allowed` WRITE;
/*!40000 ALTER TABLE `parcels_allowed` DISABLE KEYS */;
/*!40000 ALTER TABLE `parcels_allowed` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parcels_evidences`
--

DROP TABLE IF EXISTS `parcels_evidences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `parcels_evidences` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `parcel_id` int(11) NOT NULL,
  `parcel_package_id` int(11) NOT NULL,
  `parcel_incidence_id` int(11) DEFAULT NULL,
  `file_evidence` varchar(254) NOT NULL,
  `file_type` enum('img','file') NOT NULL,
  `notes` text,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `parcels_evidences_parcel_id_fk` (`parcel_id`),
  KEY `parcels_evidences_parcel_package_id_fk` (`parcel_package_id`),
  KEY `parcels_evidences_incidence_id_fk` (`parcel_incidence_id`),
  CONSTRAINT `parcels_evidences_parcel_id_fk` FOREIGN KEY (`parcel_id`) REFERENCES `parcels` (`id`),
  CONSTRAINT `parcels_evidences_parcel_incidence_id_fk` FOREIGN KEY (`parcel_incidence_id`) REFERENCES `incidences` (`id`),
  CONSTRAINT `parcels_evidences_parcel_package_id_fk` FOREIGN KEY (`parcel_package_id`) REFERENCES `parcels_packages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parcels_evidences`
--

LOCK TABLES `parcels_evidences` WRITE;
/*!40000 ALTER TABLE `parcels_evidences` DISABLE KEYS */;
/*!40000 ALTER TABLE `parcels_evidences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parcels_incidences`
--

DROP TABLE IF EXISTS `parcels_incidences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `parcels_incidences` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `parcel_id` int(11) NOT NULL,
  `parcel_package_id` int(11) NOT NULL,
  `incidence_id` int(11) NOT NULL,
  `notes` text DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `parcels_incidences_parcel_id_fk` (`parcel_id`),
  KEY `parcels_incidences_parcel_package_id_fk` (`parcel_package_id`),
  KEY `parcels_incidences_incidence_id_fk` (`incidence_id`),
  CONSTRAINT `parcels_incidences_incidences_id_fk` FOREIGN KEY (`incidence_id`) REFERENCES `incidences` (`id`),
  CONSTRAINT `parcels_incidences_parcel_id_fk` FOREIGN KEY (`parcel_id`) REFERENCES `parcels` (`id`),
  CONSTRAINT `parcels_incidences_parcel_package_id_fk` FOREIGN KEY (`parcel_package_id`) REFERENCES `parcels_packages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parcels_incidences`
--

LOCK TABLES `parcels_incidences` WRITE;
/*!40000 ALTER TABLE `parcels_incidences` DISABLE KEYS */;
/*!40000 ALTER TABLE `parcels_incidences` ENABLE KEYS */;
UNLOCK TABLES;

DROP TABLE IF EXISTS `package_types`;
CREATE TABLE package_types (
  id int(11) NOT NULL AUTO_INCREMENT,
  name varchar(50) NOT NULL,
  status tinyint(4) NOT NULL DEFAULT '1',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by int(11) DEFAULT NULL,
  updated_at datetime DEFAULT NULL,
  updated_by int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
);
--
-- Table structure for table `parcels_packages`
--

DROP TABLE IF EXISTS `parcels_packages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `parcels_packages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `parcel_id` int(11) NOT NULL,
  `shipping_type` enum('parcel','courier') NOT NULL DEFAULT 'parcel',
  `is_valid` tinyint(1) NOT NULL DEFAULT '1',
  `need_auth` tinyint(1) NOT NULL DEFAULT '0',
  `auth_by` int(11) DEFAULT NULL,
  `auth_code` varchar(60) DEFAULT NULL,
  `auth_status` tinyint(4) NOT NULL DEFAULT '0',
  `package_status` int(11) NOT NULL DEFAULT '0',
  `parcel_allowed_id` int(11) DEFAULT NULL,
  `need_documentation` tinyint(1) NOT NULL DEFAULT '0',
  `package_code` varchar(60) DEFAULT NULL,
  `package_type_id` INT(11) DEFAULT NULL,
  `package_price_id` INT(11) DEFAULT NULL,
  `price` decimal(12,2) NOT NULL DEFAULT '0.0',
  `package_price_km_id` INT(11) DEFAULT NULL,
  `price_km` decimal(12,2) NOT NULL DEFAULT '0.0',
  `amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `discount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `discount_code_id` int(11) DEFAULT NULL,
  `has_insurance` tinyint(1) NOT NULL DEFAULT '0',
  `insurance_value` decimal(12,2) NOT NULL DEFAULT '0.00',
  `insurance_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `weight` decimal(12,2) DEFAULT NULL,
  `height` decimal(12,2) DEFAULT NULL,
  `width` decimal(12,2) DEFAULT NULL,
  `length` decimal(12,2) DEFAULT NULL,
  `notes` text,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `schedule_route_destination_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `parcels_packages_parcel_id_fk` (`parcel_id`),
  KEY `parcels_packages_parcel_allowed_id_fk` (`parcel_allowed_id`),
  KEY `fk_parcels_packages_schedule_route_destination_id` (`schedule_route_destination_id`),
  KEY `parcels_packages_package_price_id_fk` (`package_price_id`),
  KEY `parcels_packages_package_price_km_id_fk` (`package_price_km_id`),
  KEY `parcels_packages_package_type_id_fk` (`package_type_id`),
  CONSTRAINT `parcels_packages_package_type_id_fk` FOREIGN KEY (`package_type_id`) REFERENCES `package_types` (`id`),
  CONSTRAINT `parcels_packages_package_price_id_fk` FOREIGN KEY (`package_price_id`) REFERENCES `package_price` (`id`),
  CONSTRAINT `parcels_packages_package_price_km_id_fk` FOREIGN KEY (`package_price_km_id`) REFERENCES `package_price_km` (`id`),
  CONSTRAINT `parcels_packages_parcel_allowed_id_fk` FOREIGN KEY (`parcel_allowed_id`) REFERENCES `parcels_allowed` (`id`),
  CONSTRAINT `parcels_packages_parcel_id_fk` FOREIGN KEY (`parcel_id`) REFERENCES `parcels` (`id`),
  CONSTRAINT `fk_parcels_packages_schedule_route_destination_id` FOREIGN KEY (`schedule_route_destination_id`) REFERENCES `schedule_route_destination` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parcels_packages`
--

LOCK TABLES `parcels_packages` WRITE;
/*!40000 ALTER TABLE `parcels_packages` DISABLE KEYS */;
/*!40000 ALTER TABLE `parcels_packages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `parcels_packages_tracking`
--

DROP TABLE IF EXISTS `parcels_packages_tracking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `parcels_packages_tracking` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `parcel_id` int(11) NOT NULL,
  `parcel_package_id` int(11) NOT NULL,
  `action` enum('register','paid','move','intransit','loaded','downloaded','incidence','canceled','closed') NOT NULL,
  `notes` text,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `parcels_packages_tracking_parcel_id_fk` (`parcel_id`),
  KEY `parcels_packages_tracking_parcel_package_id_fk` (`parcel_package_id`),
  CONSTRAINT `parcels_packages_tracking_parcel_id_fk` FOREIGN KEY (`parcel_id`) REFERENCES `parcels` (`id`),
  CONSTRAINT `parcels_packages_tracking_parcel_package_id_fk` FOREIGN KEY (`parcel_package_id`) REFERENCES `parcels_packages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `parcels_packages_tracking`
--

LOCK TABLES `parcels_packages_tracking` WRITE;
/*!40000 ALTER TABLE `parcels_packages_tracking` DISABLE KEYS */;
/*!40000 ALTER TABLE `parcels_packages_tracking` ENABLE KEYS */;
UNLOCK TABLES;



DROP TABLE IF EXISTS `parcels_packings`;

CREATE TABLE parcels_packings (
  id int(11) NOT NULL AUTO_INCREMENT,
  parcel_id int(11) NOT NULL,
  packing_id int(11) NOT NULL,
  quantity int(11) NOT NULL,
  unit_price decimal(12,2) NOT NULL DEFAULT '0.00',
  amount decimal(12,2) NOT NULL DEFAULT '0.00',
  discount decimal(12,2) NOT NULL DEFAULT '0.00',
  discount_code_id int(11) DEFAULT NULL,
  total_amount decimal(12,2) NOT NULL DEFAULT '0.00',
  status tinyint(4) NOT NULL DEFAULT '1',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by int(11) DEFAULT NULL,
  updated_at datetime DEFAULT NULL,
  updated_by int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_parcels_packings_parcel_id` (`parcel_id`),
  CONSTRAINT `fk_parcels_packings_parcel_id` FOREIGN KEY (`parcel_id`) REFERENCES `parcels` (`id`),
  KEY `fk_parcels_packings_packing_id` (`packing_id`),
  CONSTRAINT `fk_parcels_packings_packing_id` FOREIGN KEY (`packing_id`) REFERENCES `packings` (`id`)
);

--
-- Table structure for table `payment`
--

DROP TABLE IF EXISTS `payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `payment` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `boarding_pass_id` int(11) DEFAULT NULL,
  `rental_id` int(11) DEFAULT NULL,
  `parcel_id` INT(11) NULL DEFAULT NULL,
  `payment_method_id` int(11) NOT NULL,
  `payment_method` enum('cash','card', 'check', 'transfer', 'deposit') NOT NULL DEFAULT 'cash',
  `amount` decimal(12,2) NOT NULL DEFAULT '1.00',
  `reference` varchar(254) DEFAULT NULL,
  `exchange_rate_id` int(11) DEFAULT NULL,
  `currency_id` int(11) DEFAULT NULL,
  `payment_status` int(11) NOT NULL DEFAULT '1',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `is_deposit` tinyint(1) NOT NULL DEFAULT '0',
  `ticket_id` int(11) DEFAULT NULL,
  `is_extra_charge` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_peyment_boarding_pass_id` (`boarding_pass_id`),
  KEY `fk_peyment_rental_id` (`rental_id`),
  KEY `fk_peyment_payment_method_id` (`payment_method_id`),
  KEY `fk_peyment_exchange_rate_id` (`exchange_rate_id`),
  KEY `fk_peyment_currency_id` (`currency_id`),
  KEY `fk_payment_ticket_id` (`ticket_id`),
  KEY `fk_payment_parcel_id` (`parcel_id`),
  CONSTRAINT `fk_payment_ticket_id` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`),
  CONSTRAINT `fk_peyment_boarding_pass_id` FOREIGN KEY (`boarding_pass_id`) REFERENCES `boarding_pass` (`id`),
  CONSTRAINT `fk_peyment_currency_id` FOREIGN KEY (`currency_id`) REFERENCES `currency` (`id`),
  CONSTRAINT `fk_peyment_exchange_rate_id` FOREIGN KEY (`exchange_rate_id`) REFERENCES `exchange_rate` (`id`),
  CONSTRAINT `fk_peyment_payment_method_id` FOREIGN KEY (`payment_method_id`) REFERENCES `payment_method` (`id`),
  CONSTRAINT `fk_peyment_rental_id` FOREIGN KEY (`rental_id`) REFERENCES `rental` (`id`),
  CONSTRAINT `fk_payment_parcel_id` FOREIGN KEY (`parcel_id`) REFERENCES `parcels` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment`
--

LOCK TABLES `payment` WRITE;
/*!40000 ALTER TABLE `payment` DISABLE KEYS */;
/*!40000 ALTER TABLE `payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment_method`
--

DROP TABLE IF EXISTS `payment_method`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `payment_method` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `is_cash` tinyint(1) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `icon` varchar(100) NOT NULL DEFAULT 'mdi mdi-coin',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `paysheet_type`
--

DROP TABLE IF EXISTS `paysheet_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `paysheet_type` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  `type_payment` enum('event','hour','day','weekend','biweekly_pay') NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;



--
-- Table structure for table `rental`
--

DROP TABLE IF EXISTS `rental`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rental` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `first_name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `phone` varchar(10) NOT NULL,
  `email` varchar(200) DEFAULT NULL,
  `no_credential` varchar(30) DEFAULT NULL,
  `file_credential` varchar(254) DEFAULT NULL,
  `customer_id` int(11) DEFAULT NULL,
  `is_customer` tinyint(1) NOT NULL DEFAULT '0',
  `departure_date` date NOT NULL,
  `departure_time` varchar(5) NOT NULL,
  `return_date` date NOT NULL,
  `return_time` varchar(5) NOT NULL,
  `pickup_at_office` tinyint(1) NOT NULL DEFAULT '1',
  `branchoffice_id` int(11) DEFAULT NULL,
  `init_route` varchar(254) DEFAULT NULL,
  `init_full_address` varchar(254) DEFAULT NULL,
  `farthest_route` text NOT NULL,
  `farthest_full_address` varchar(254) NOT NULL,
  `total_passengers` int(11) NOT NULL DEFAULT '1',
  `vehicle_id` int(11) DEFAULT NULL,
  `has_driver` tinyint(1) NOT NULL DEFAULT '0',
  `driver_cost` decimal(12,2) NOT NULL DEFAULT '0.00',
  `extra_charges` decimal(12,2) NOT NULL DEFAULT '0.00',
  `amount` decimal(12,2) NOT NULL,
  `discount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total_amount` decimal(12,2) NOT NULL,
  `discount_code` decimal(12,2) NOT NULL DEFAULT '0.00',
  `reservation_code` varchar(16) DEFAULT NULL,
  `rent_status` int(11) NOT NULL DEFAULT '1',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `pickup_city_id` int(11) DEFAULT NULL,
  `leave_at_office` tinyint(1) NOT NULL DEFAULT '1',
  `leave_branchoffice_id` int(11) DEFAULT NULL,
  `leave_city_id` int(11) DEFAULT NULL,
  `kilometers_init` int(11) DEFAULT NULL,
  `kilometers_end` int(11) DEFAULT NULL,
  `is_quotation` tinyint(1) NOT NULL DEFAULT '0',
  `quotation_id` int(11) DEFAULT NULL,
  `delivered_at` datetime DEFAULT NULL,
  `delivered_by` int(11) DEFAULT NULL,
  `received_at` datetime DEFAULT NULL,
  `received_by` int(11) DEFAULT NULL,
  `status_on_reception` enum('0','1','2','3') NOT NULL DEFAULT '0',
  `destiny_city_id` int(11) DEFAULT NULL,
  `pickup_branchoffice_id` int(11) DEFAULT NULL,
  `pickup_route` varchar(254) DEFAULT NULL,
  `pickup_full_address` varchar(254) DEFAULT NULL,
  `leave_route` varchar(254) DEFAULT NULL,
  `leave_full_address` varchar(254) DEFAULT NULL,
  `rental_price` decimal(12,2) DEFAULT NULL,
  `rental_price_id` int(11) DEFAULT NULL,
  `guarantee_deposit` decimal(12,2) NOT NULL DEFAULT '0.00',
  `rental_minamount_percent` decimal(6,2) NOT NULL DEFAULT '0.00',
  `quotation_expired_after` int(11) NOT NULL DEFAULT '0',
  `payment_status` enum('0','1','2') NOT NULL DEFAULT '0',
  `purchase_origin` enum('sucursal','web','kiosko','app cliente') NOT NULL DEFAULT 'sucursal',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_rental_reservation_code` (`reservation_code`),
  KEY `fk_rental_customer` (`customer_id`),
  KEY `fk_rental_vehicle` (`vehicle_id`),
  KEY `fk_rental_branchoffice` (`branchoffice_id`),
  KEY `fk_pickup_city_id` (`pickup_city_id`),
  KEY `fk_leave_branchoffice_id` (`leave_branchoffice_id`),
  KEY `fk_leave_city_id` (`leave_city_id`),
  KEY `fk_pickup_branchoffice` (`pickup_branchoffice_id`),
  KEY `fk_rental_destiny_city_id` (`destiny_city_id`),
  CONSTRAINT `fk_leave_branchoffice_id` FOREIGN KEY (`leave_branchoffice_id`) REFERENCES `branchoffice` (`id`),
  CONSTRAINT `fk_leave_city_id` FOREIGN KEY (`leave_city_id`) REFERENCES `city` (`id`),
  CONSTRAINT `fk_pickup_branchoffice` FOREIGN KEY (`pickup_branchoffice_id`) REFERENCES `branchoffice` (`id`),
  CONSTRAINT `fk_pickup_city_id` FOREIGN KEY (`pickup_city_id`) REFERENCES `city` (`id`),
  CONSTRAINT `fk_rental_branchoffice` FOREIGN KEY (`branchoffice_id`) REFERENCES `branchoffice` (`id`),
  CONSTRAINT `fk_rental_customer` FOREIGN KEY (`customer_id`) REFERENCES `customer` (`id`),
  CONSTRAINT `fk_rental_destiny_city_id` FOREIGN KEY (`destiny_city_id`) REFERENCES `city` (`id`),
  CONSTRAINT `fk_rental_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rental`
--

LOCK TABLES `rental` WRITE;
/*!40000 ALTER TABLE `rental` DISABLE KEYS */;
/*!40000 ALTER TABLE `rental` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rental_config_vehicle`
--

DROP TABLE IF EXISTS `rental_config_vehicle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rental_config_vehicle` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `rental_id` int(11) NOT NULL,
  `addon_vehicle_id` int(11) NOT NULL,
  `use_price` decimal(12,2) NOT NULL DEFAULT '0.00',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `status_on_reception` enum('0','1','2','3') NOT NULL DEFAULT '0',
  `notes` varchar(8000) DEFAULT NULL,
  `damage_percent` decimal(12,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_rental_config_vehicle_rental` (`rental_id`),
  KEY `fk_rental_config_vehicle_addon_vehicle` (`addon_vehicle_id`),
  CONSTRAINT `fk_rental_config_vehicle_addon_vehicle` FOREIGN KEY (`addon_vehicle_id`) REFERENCES `addon_vehicle` (`id`),
  CONSTRAINT `fk_rental_config_vehicle_rental` FOREIGN KEY (`rental_id`) REFERENCES `rental` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rental_config_vehicle`
--

LOCK TABLES `rental_config_vehicle` WRITE;
/*!40000 ALTER TABLE `rental_config_vehicle` DISABLE KEYS */;
/*!40000 ALTER TABLE `rental_config_vehicle` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rental_driver`
--

DROP TABLE IF EXISTS `rental_driver`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rental_driver` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `rental_id` int(11) NOT NULL,
  `employee_id` int(11) DEFAULT NULL,
  `first_name` varchar(50) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `birthday` date DEFAULT NULL,
  `no_licence` varchar(30) NOT NULL,
  `expired_at` date NOT NULL,
  `file_licence` varchar(254) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_rental_driver_rental` (`rental_id`),
  KEY `fk_rental_driver_employee` (`employee_id`),
  CONSTRAINT `fk_rental_driver_employee` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`),
  CONSTRAINT `fk_rental_driver_rental` FOREIGN KEY (`rental_id`) REFERENCES `rental` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rental_driver`
--

LOCK TABLES `rental_driver` WRITE;
/*!40000 ALTER TABLE `rental_driver` DISABLE KEYS */;
/*!40000 ALTER TABLE `rental_driver` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rental_evidence`
--

DROP TABLE IF EXISTS `rental_evidence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rental_evidence` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `rental_id` int(11) NOT NULL,
  `file_evidence` varchar(254) DEFAULT NULL,
  `on_departure` tinyint(1) NOT NULL DEFAULT '1',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_rental_evidence_rental` (`rental_id`),
  CONSTRAINT `fk_rental_evidence_rental` FOREIGN KEY (`rental_id`) REFERENCES `rental` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rental_evidence`
--

LOCK TABLES `rental_evidence` WRITE;
/*!40000 ALTER TABLE `rental_evidence` DISABLE KEYS */;
/*!40000 ALTER TABLE `rental_evidence` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rental_extra_charge`
--

DROP TABLE IF EXISTS `rental_extra_charge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rental_extra_charge` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `rental_id` int(11) NOT NULL,
  `extra_charge_id` int(11) DEFAULT NULL,
  `reference` varchar(254) DEFAULT NULL,
  `on_reception` tinyint(1) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `amount` decimal(12,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_rental_extra_charge_rental` (`rental_id`),
  KEY `fk_rental_extra_charge_extra_charge` (`extra_charge_id`),
  CONSTRAINT `fk_rental_extra_charge_extra_charge` FOREIGN KEY (`extra_charge_id`) REFERENCES `extra_charge` (`id`),
  CONSTRAINT `fk_rental_extra_charge_rental` FOREIGN KEY (`rental_id`) REFERENCES `rental` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rental_extra_charge`
--

LOCK TABLES `rental_extra_charge` WRITE;
/*!40000 ALTER TABLE `rental_extra_charge` DISABLE KEYS */;
/*!40000 ALTER TABLE `rental_extra_charge` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rental_price`
--

DROP TABLE IF EXISTS `rental_price`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rental_price` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `vehicle_id` int(11) NOT NULL,
  `price` decimal(12,2) DEFAULT NULL,
  `type_price` enum('D','H','K') NOT NULL DEFAULT 'D',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `guarantee_deposit` decimal(12,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`id`),
  KEY `fk_rental_price_vehicle_id` (`vehicle_id`),
  CONSTRAINT `fk_rental_price_vehicle_id` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rental_price`
--

LOCK TABLES `rental_price` WRITE;
/*!40000 ALTER TABLE `rental_price` DISABLE KEYS */;
/*!40000 ALTER TABLE `rental_price` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `requirement`
--

DROP TABLE IF EXISTS `requirement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `requirement` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `is_group` tinyint(1) NOT NULL DEFAULT '0',
  `is_recurrent` tinyint(1) NOT NULL DEFAULT '0',
  `parent_id` int(11) DEFAULT NULL,
  `name` varchar(150) NOT NULL,
  `description` varchar(254) DEFAULT NULL,
  `is_required` tinyint(1) NOT NULL DEFAULT '1',
  `type_req` enum('date','string','number','boolean','select','multiple','file','') DEFAULT '',
  `type_values` text,
  `order_req` int(11) DEFAULT NULL,
  `recurrent_type` enum('date','days') DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `parent_id` (`parent_id`),
  CONSTRAINT `requirement_ibfk_2` FOREIGN KEY (`parent_id`) REFERENCES `requirement` (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `schedule_route`
--

DROP TABLE IF EXISTS `schedule_route`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `schedule_route` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `vehicle_id` int(11) NOT NULL,
  `travel_date` datetime NOT NULL,
  `arrival_date` datetime NOT NULL,
  `config_route_id` int(11) NOT NULL,
  `config_schedule_id` int(11) NOT NULL,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `schedule_status` int(11) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `vehicle_id` (`vehicle_id`),
  KEY `config_route_id` (`config_route_id`),
  KEY `config_schedule_id` (`config_schedule_id`),
  CONSTRAINT `schedule_route_ibfk_1` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`),
  CONSTRAINT `schedule_route_ibfk_2` FOREIGN KEY (`config_route_id`) REFERENCES `config_route` (`id`),
  CONSTRAINT `schedule_route_ibfk_3` FOREIGN KEY (`config_schedule_id`) REFERENCES `config_schedule` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule_route`
--

LOCK TABLES `schedule_route` WRITE;
/*!40000 ALTER TABLE `schedule_route` DISABLE KEYS */;
/*!40000 ALTER TABLE `schedule_route` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedule_route_destination`
--

DROP TABLE IF EXISTS `schedule_route_destination`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `schedule_route_destination` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `schedule_route_id` int(11) NOT NULL,
  `config_destination_id` int(11) NOT NULL,
  `terminal_origin_id` int(11) NOT NULL,
  `terminal_destiny_id` int(11) NOT NULL,
  `travel_date` datetime NOT NULL,
  `arrival_date` datetime NOT NULL,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `destination_status` int(11) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_schedule_route_destination_sr_id` (`schedule_route_id`),
  KEY `fk_schedule_route_destination_cd_id` (`config_destination_id`),
  KEY `fk_schedule_route_destination_to_id` (`terminal_origin_id`),
  KEY `fk_schedule_route_destination_td_id` (`terminal_destiny_id`),
  CONSTRAINT `fk_schedule_route_destination_cd_id` FOREIGN KEY (`config_destination_id`) REFERENCES `config_destination` (`id`),
  CONSTRAINT `fk_schedule_route_destination_sr_id` FOREIGN KEY (`schedule_route_id`) REFERENCES `schedule_route` (`id`),
  CONSTRAINT `fk_schedule_route_destination_td_id` FOREIGN KEY (`terminal_destiny_id`) REFERENCES `branchoffice` (`id`),
  CONSTRAINT `fk_schedule_route_destination_to_id` FOREIGN KEY (`terminal_origin_id`) REFERENCES `branchoffice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule_route_destination`
--

LOCK TABLES `schedule_route_destination` WRITE;
/*!40000 ALTER TABLE `schedule_route_destination` DISABLE KEYS */;
/*!40000 ALTER TABLE `schedule_route_destination` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `schedule_route_driver`
--

DROP TABLE IF EXISTS `schedule_route_driver`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `schedule_route_driver` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `schedule_route_id` int(11) NOT NULL,
  `employee_id` int(11) NOT NULL,
  `time_tracking` varchar(5) NOT NULL,
  `terminal_origin_id` int(11) NOT NULL,
  `terminal_destiny_id` int(11) NOT NULL,
  `was_completed` tinyint(1) DEFAULT NULL,
  `driver_status` enum('0','1','2','3','4') NOT NULL DEFAULT '1',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `schedule_route_id` (`schedule_route_id`),
  KEY `employee_id` (`employee_id`),
  KEY `terminal_origin_id` (`terminal_origin_id`),
  KEY `terminal_destiny_id` (`terminal_destiny_id`),
  CONSTRAINT `schedule_route_driver_ibfk_1` FOREIGN KEY (`schedule_route_id`) REFERENCES `schedule_route` (`id`),
  CONSTRAINT `schedule_route_driver_ibfk_2` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`),
  CONSTRAINT `schedule_route_driver_ibfk_3` FOREIGN KEY (`terminal_origin_id`) REFERENCES `branchoffice` (`id`),
  CONSTRAINT `schedule_route_driver_ibfk_4` FOREIGN KEY (`terminal_destiny_id`) REFERENCES `branchoffice` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `schedule_route_driver`
--

LOCK TABLES `schedule_route_driver` WRITE;
/*!40000 ALTER TABLE `schedule_route_driver` DISABLE KEYS */;
/*!40000 ALTER TABLE `schedule_route_driver` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `special_ticket`
--

DROP TABLE IF EXISTS `special_ticket`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `special_ticket` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(30) NOT NULL,
  `description` text,
  `has_discount` tinyint(1) NOT NULL DEFAULT '0',
  `total_discount` int(2) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `base` tinyint(1) NOT NULL DEFAULT '0',
  `available_tickets` int(11) NOT NULL DEFAULT '-1',
  `has_preferent_zone` tinyint(1) NOT NULL DEFAULT '0',
  `older_than` INT(3) NOT NULL DEFAULT '1',
  `younger_than` INT(3) NOT NULL DEFAULT '120',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `state`
--

DROP TABLE IF EXISTS `state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `state` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `country_id` int(11) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `country_id` (`country_id`),
  CONSTRAINT `state_ibfk_1` FOREIGN KEY (`country_id`) REFERENCES `country` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `street`
--

DROP TABLE IF EXISTS `street`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `street` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` text NOT NULL,
  `county_id` int(11) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `municipality_id` (`county_id`),
  CONSTRAINT `street_ibfk_1` FOREIGN KEY (`county_id`) REFERENCES `county` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `street`
--

LOCK TABLES `street` WRITE;
/*!40000 ALTER TABLE `street` DISABLE KEYS */;
/*!40000 ALTER TABLE `street` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `suburb`
--

DROP TABLE IF EXISTS `suburb`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `suburb` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(150) NOT NULL,
  `county_id` int(11) NOT NULL,
  `suburb_type` varchar(100) NOT NULL,
  `zip_code` int(11) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `city_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `municipality_id` (`county_id`),
  KEY `fk_suburb_city` (`city_id`),
  CONSTRAINT `fk_suburb_city` FOREIGN KEY (`city_id`) REFERENCES `city` (`id`),
  CONSTRAINT `suburb_ibfk_1` FOREIGN KEY (`county_id`) REFERENCES `county` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=138896 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `supplier`
--

DROP TABLE IF EXISTS `supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `supplier` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `supplier_type` enum('producto','servicio') NOT NULL,
  `legal_person` enum('fisico','moral') NOT NULL,
  `address` varchar(255) NOT NULL,
  `street_id` int(11) NOT NULL,
  `no_ext` varchar(15) DEFAULT NULL,
  `no_int` varchar(15) DEFAULT NULL,
  `suburb_id` int(11) NOT NULL,
  `city_id` int(11) NOT NULL,
  `county_id` int(11) NOT NULL,
  `state_id` int(11) NOT NULL,
  `country_id` int(11) NOT NULL,
  `zip_code` int(11) DEFAULT NULL,
  `reference` varchar(254) DEFAULT NULL,
  `phone` varchar(10) DEFAULT NULL,
  `email` varchar(150) DEFAULT NULL,
  `notes` varchar(254) DEFAULT NULL,
  `has_credit` tinyint(1) DEFAULT NULL,
  `credit_limit` decimal(12,2) DEFAULT NULL,
  `credit_time_limit` int(11) DEFAULT NULL,
  `has_discounts` tinyint(1) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `business_name` varchar(100) NOT NULL,
  `rfc` varchar(13) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `discount` decimal(12,2) DEFAULT NULL,
  `accounting_acount` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_supplier_street` (`street_id`),
  KEY `fk_supplier_suburb` (`suburb_id`),
  KEY `fk_supplier_city` (`city_id`),
  KEY `fk_supplier_county` (`county_id`),
  KEY `fk_supplier_state` (`state_id`),
  KEY `fk_supplier_country` (`country_id`),
  CONSTRAINT `fk_supplier_city` FOREIGN KEY (`city_id`) REFERENCES `city` (`id`),
  CONSTRAINT `fk_supplier_country` FOREIGN KEY (`country_id`) REFERENCES `country` (`id`),
  CONSTRAINT `fk_supplier_county` FOREIGN KEY (`county_id`) REFERENCES `county` (`id`),
  CONSTRAINT `fk_supplier_state` FOREIGN KEY (`state_id`) REFERENCES `state` (`id`),
  CONSTRAINT `fk_supplier_street` FOREIGN KEY (`street_id`) REFERENCES `street` (`id`),
  CONSTRAINT `fk_supplier_suburb` FOREIGN KEY (`suburb_id`) REFERENCES `suburb` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supplier`
--

LOCK TABLES `supplier` WRITE;
/*!40000 ALTER TABLE `supplier` DISABLE KEYS */;
/*!40000 ALTER TABLE `supplier` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supplier_bank_info`
--

DROP TABLE IF EXISTS `supplier_bank_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `supplier_bank_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `supplier_id` int(11) NOT NULL,
  `bank` varchar(30) DEFAULT NULL,
  `account` varchar(30) NOT NULL,
  `clabe` varchar(30) DEFAULT NULL,
  `reference` varchar(254) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `currency_id` int(11) NOT NULL,
  `is_default` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_supplier_bank_info_supplier` (`supplier_id`),
  CONSTRAINT `fk_supplier_bank_info_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supplier_bank_info`
--

LOCK TABLES `supplier_bank_info` WRITE;
/*!40000 ALTER TABLE `supplier_bank_info` DISABLE KEYS */;
/*!40000 ALTER TABLE `supplier_bank_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supplier_contact`
--

DROP TABLE IF EXISTS `supplier_contact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `supplier_contact` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `supplier_id` int(11) NOT NULL,
  `name` varchar(30) NOT NULL,
  `last_name` varchar(30) NOT NULL,
  `job` varchar(30) DEFAULT NULL,
  `phone` varchar(10) NOT NULL,
  `email` varchar(100) NOT NULL,
  `notes` varchar(254) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_supplier_contact_supplier` (`supplier_id`),
  CONSTRAINT `fk_supplier_contact_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supplier_contact`
--

LOCK TABLES `supplier_contact` WRITE;
/*!40000 ALTER TABLE `supplier_contact` DISABLE KEYS */;
/*!40000 ALTER TABLE `supplier_contact` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tickets`
--

DROP TABLE IF EXISTS `tickets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tickets` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `cash_out_id` int(11) NOT NULL,
  `ticket_code` varchar(60) DEFAULT NULL,
  `iva` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total` decimal(12,2) NOT NULL DEFAULT '0.00',
  `paid` decimal(12,2) NOT NULL DEFAULT '0.00',
  `paid_change` decimal(12,2) NOT NULL DEFAULT '0.00',
  `was_printed` tinyint(1) NOT NULL DEFAULT '0',
  `status` int NOT NULL DEFAULT 1,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_tickets_cash_out_id` (`cash_out_id`),
  CONSTRAINT `fk_tickets_cash_out_id` FOREIGN KEY (`cash_out_id`) REFERENCES `cash_out` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tickets`
--

LOCK TABLES `tickets` WRITE;
/*!40000 ALTER TABLE `tickets` DISABLE KEYS */;
/*!40000 ALTER TABLE `tickets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tickets_details`
--

DROP TABLE IF EXISTS `tickets_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tickets_details` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ticket_id` int(11) NOT NULL,
  `quantity` int(11) DEFAULT NULL,
  `detail` text,
  `unit_price` decimal(12,2) NOT NULL DEFAULT '0.00',
  `amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_tickets_details_ticket_id` (`ticket_id`),
  CONSTRAINT `fk_tickets_details_ticket_id` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tickets_details`
--

LOCK TABLES `tickets_details` WRITE;
/*!40000 ALTER TABLE `tickets_details` DISABLE KEYS */;
/*!40000 ALTER TABLE `tickets_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `time_zone`
--

DROP TABLE IF EXISTS `time_zone`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `time_zone` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `offset` varchar(10) NOT NULL,
  `canonical_id` varchar(100) NOT NULL,
  `alianses` varchar(255) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1294 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `time_zone`
--

LOCK TABLES `time_zone` WRITE;
/*!40000 ALTER TABLE `time_zone` DISABLE KEYS */;
INSERT INTO `time_zone` VALUES (863,'-12:00','Etc/GMT+12','',1,'2018-07-25 09:43:55',1,NULL,NULL),(864,'-11:00','Etc/GMT+11','',1,'2018-07-25 09:43:55',1,NULL,NULL),(865,'-11:00','Pacific/Apia','',1,'2018-07-25 09:43:55',1,NULL,NULL),(866,'-11:00','Pacific/Midway','',1,'2018-07-25 09:43:55',1,NULL,NULL),(867,'-11:00','Pacific/Niue','',1,'2018-07-25 09:43:55',1,NULL,NULL),(868,'-11:00','Pacific/Pago_Pago','Pacific/Samoa, US/Samoa',1,'2018-07-25 09:43:55',1,NULL,NULL),(869,'-10:00','America/Adak','America/Atka, US/Aleutian',1,'2018-07-25 09:43:55',1,NULL,NULL),(870,'-10:00','Etc/GMT+10','',1,'2018-07-25 09:43:55',1,NULL,NULL),(871,'-10:00','HST','',1,'2018-07-25 09:43:55',1,NULL,NULL),(872,'-10:00','Pacific/Fakaofo','',1,'2018-07-25 09:43:55',1,NULL,NULL),(873,'-10:00','Pacific/Honolulu','US/Hawaii',1,'2018-07-25 09:43:55',1,NULL,NULL),(874,'-10:00','Pacific/Johnston','',1,'2018-07-25 09:43:55',1,NULL,NULL),(875,'-10:00','Pacific/Rarotonga','',1,'2018-07-25 09:43:55',1,NULL,NULL),(876,'-10:00','Pacific/Tahiti','',1,'2018-07-25 09:43:55',1,NULL,NULL),(877,'-09:30','Pacific/Marquesas','',1,'2018-07-25 09:43:55',1,NULL,NULL),(878,'-09:00','America/Anchorage','US/Alaska',1,'2018-07-25 09:43:55',1,NULL,NULL),(879,'-09:00','America/Juneau','',1,'2018-07-25 09:43:55',1,NULL,NULL),(880,'-09:00','America/Nome','',1,'2018-07-25 09:43:55',1,NULL,NULL),(881,'-09:00','America/Yakutat','',1,'2018-07-25 09:43:55',1,NULL,NULL),(882,'-09:00','Etc/GMT+9','',1,'2018-07-25 09:43:55',1,NULL,NULL),(883,'-09:00','Pacific/Gambier','',1,'2018-07-25 09:43:55',1,NULL,NULL),(884,'-08:00','America/Dawson','',1,'2018-07-25 09:43:55',1,NULL,NULL),(885,'-08:00','America/Los_Angeles','US/Pacific, US/Pacific-New',1,'2018-07-25 09:43:55',1,NULL,NULL),(886,'-08:00','America/Santa_Isabel','',1,'2018-07-25 09:43:55',1,NULL,NULL),(887,'-08:00','America/Tijuana','America/Ensenada, Mexico/BajaNorte',1,'2018-07-25 09:43:55',1,NULL,NULL),(888,'-08:00','America/Vancouver','Canada/Pacific',1,'2018-07-25 09:43:55',1,NULL,NULL),(889,'-08:00','America/Whitehorse','Canada/Yukon',1,'2018-07-25 09:43:55',1,NULL,NULL),(890,'-08:00','Etc/GMT+8','',1,'2018-07-25 09:43:55',1,NULL,NULL),(891,'-08:00','PST8PDT','',1,'2018-07-25 09:43:55',1,NULL,NULL),(892,'-08:00','Pacific/Pitcairn','',1,'2018-07-25 09:43:55',1,NULL,NULL),(893,'-07:00','America/Boise','',1,'2018-07-25 09:43:55',1,NULL,NULL),(894,'-07:00','America/Cambridge_Bay','',1,'2018-07-25 09:43:55',1,NULL,NULL),(895,'-07:00','America/Chihuahua','',1,'2018-07-25 09:43:55',1,NULL,NULL),(896,'-07:00','America/Dawson_Creek','',1,'2018-07-25 09:43:55',1,NULL,NULL),(897,'-07:00','America/Denver','America/Shiprock, Navajo, US/Mountain',1,'2018-07-25 09:43:55',1,NULL,NULL),(898,'-07:00','America/Edmonton','Canada/Mountain',1,'2018-07-25 09:43:55',1,NULL,NULL),(899,'-07:00','America/Hermosillo','',1,'2018-07-25 09:43:55',1,NULL,NULL),(900,'-07:00','America/Inuvik','',1,'2018-07-25 09:43:56',1,NULL,NULL),(901,'-07:00','America/Mazatlan','Mexico/BajaSur',1,'2018-07-25 09:43:56',1,NULL,NULL),(902,'-07:00','America/Ojinaga','',1,'2018-07-25 09:43:56',1,NULL,NULL),(903,'-07:00','America/Phoenix','US/Arizona',1,'2018-07-25 09:43:56',1,NULL,NULL),(904,'-07:00','America/Yellowknife','',1,'2018-07-25 09:43:56',1,NULL,NULL),(905,'-07:00','Etc/GMT+7','',1,'2018-07-25 09:43:56',1,NULL,NULL),(906,'-07:00','MST','',1,'2018-07-25 09:43:56',1,NULL,NULL),(907,'-07:00','MST7MDT','',1,'2018-07-25 09:43:56',1,NULL,NULL),(908,'-06:00','America/Bahia_Banderas','',1,'2018-07-25 09:43:56',1,NULL,NULL),(909,'-06:00','America/Belize','',1,'2018-07-25 09:43:56',1,NULL,NULL),(910,'-06:00','America/Cancun','',1,'2018-07-25 09:43:56',1,NULL,NULL),(911,'-06:00','America/Chicago','US/Central',1,'2018-07-25 09:43:56',1,NULL,NULL),(912,'-06:00','America/Costa_Rica','',1,'2018-07-25 09:43:56',1,NULL,NULL),(913,'-06:00','America/El_Salvador','',1,'2018-07-25 09:43:56',1,NULL,NULL),(914,'-06:00','America/Guatemala','',1,'2018-07-25 09:43:56',1,NULL,NULL),(915,'-06:00','America/Indiana/Knox','America/Knox_IN, US/Indiana-Starke',1,'2018-07-25 09:43:56',1,NULL,NULL),(916,'-06:00','America/Indiana/Tell_City','',1,'2018-07-25 09:43:56',1,NULL,NULL),(917,'-06:00','America/Managua','',1,'2018-07-25 09:43:56',1,NULL,NULL),(918,'-06:00','America/Matamoros','',1,'2018-07-25 09:43:56',1,NULL,NULL),(919,'-06:00','America/Menominee','',1,'2018-07-25 09:43:56',1,NULL,NULL),(920,'-06:00','America/Merida','',1,'2018-07-25 09:43:56',1,NULL,NULL),(921,'-06:00','America/Mexico_City','Mexico/General',1,'2018-07-25 09:43:56',1,NULL,NULL),(922,'-06:00','America/Monterrey','',1,'2018-07-25 09:43:56',1,NULL,NULL),(923,'-06:00','America/North_Dakota/Center','',1,'2018-07-25 09:43:56',1,NULL,NULL),(924,'-06:00','America/North_Dakota/New_Salem','',1,'2018-07-25 09:43:56',1,NULL,NULL),(925,'-06:00','America/Rainy_River','',1,'2018-07-25 09:43:56',1,NULL,NULL),(926,'-06:00','America/Rankin_Inlet','',1,'2018-07-25 09:43:56',1,NULL,NULL),(927,'-06:00','America/Regina','Canada/East-Saskatchewan, Canada/Saskatchewan',1,'2018-07-25 09:43:56',1,NULL,NULL),(928,'-06:00','America/Swift_Current','',1,'2018-07-25 09:43:56',1,NULL,NULL),(929,'-06:00','America/Tegucigalpa','',1,'2018-07-25 09:43:56',1,NULL,NULL),(930,'-06:00','America/Winnipeg','Canada/Central',1,'2018-07-25 09:43:56',1,NULL,NULL),(931,'-06:00','CST6CDT','',1,'2018-07-25 09:43:56',1,NULL,NULL),(932,'-06:00','Etc/GMT+6','',1,'2018-07-25 09:43:56',1,NULL,NULL),(933,'-06:00','Pacific/Easter','Chile/EasterIsland',1,'2018-07-25 09:43:56',1,NULL,NULL),(934,'-06:00','Pacific/Galapagos','',1,'2018-07-25 09:43:56',1,NULL,NULL),(935,'-05:00','America/Atikokan','America/Coral_Harbour',1,'2018-07-25 09:43:56',1,NULL,NULL),(936,'-05:00','America/Bogota','',1,'2018-07-25 09:43:56',1,NULL,NULL),(937,'-05:00','America/Cayman','',1,'2018-07-25 09:43:56',1,NULL,NULL),(938,'-05:00','America/Detroit','US/Michigan',1,'2018-07-25 09:43:56',1,NULL,NULL),(939,'-05:00','America/Grand_Turk','',1,'2018-07-25 09:43:56',1,NULL,NULL),(940,'-05:00','America/Guayaquil','',1,'2018-07-25 09:43:56',1,NULL,NULL),(941,'-05:00','America/Havana','Cuba',1,'2018-07-25 09:43:56',1,NULL,NULL),(942,'-05:00','America/Indiana/Indianapolis','America/Fort_Wayne, America/Indianapolis, US/East-Indiana',1,'2018-07-25 09:43:56',1,NULL,NULL),(943,'-05:00','America/Indiana/Marengo','',1,'2018-07-25 09:43:56',1,NULL,NULL),(944,'-05:00','America/Indiana/Petersburg','',1,'2018-07-25 09:43:56',1,NULL,NULL),(945,'-05:00','America/Indiana/Vevay','',1,'2018-07-25 09:43:56',1,NULL,NULL),(946,'-05:00','America/Indiana/Vincennes','',1,'2018-07-25 09:43:56',1,NULL,NULL),(947,'-05:00','America/Indiana/Winamac','',1,'2018-07-25 09:43:56',1,NULL,NULL),(948,'-05:00','America/Iqaluit','',1,'2018-07-25 09:43:56',1,NULL,NULL),(949,'-05:00','America/Jamaica','Jamaica',1,'2018-07-25 09:43:56',1,NULL,NULL),(950,'-05:00','America/Kentucky/Louisville','America/Louisville',1,'2018-07-25 09:43:56',1,NULL,NULL),(951,'-05:00','America/Kentucky/Monticello','',1,'2018-07-25 09:43:56',1,NULL,NULL),(952,'-05:00','America/Lima','',1,'2018-07-25 09:43:56',1,NULL,NULL),(953,'-05:00','America/Montreal','',1,'2018-07-25 09:43:56',1,NULL,NULL),(954,'-05:00','America/Nassau','',1,'2018-07-25 09:43:56',1,NULL,NULL),(955,'-05:00','America/New_York','US/Eastern',1,'2018-07-25 09:43:56',1,NULL,NULL),(956,'-05:00','America/Nipigon','',1,'2018-07-25 09:43:56',1,NULL,NULL),(957,'-05:00','America/Panama','',1,'2018-07-25 09:43:56',1,NULL,NULL),(958,'-05:00','America/Pangnirtung','',1,'2018-07-25 09:43:56',1,NULL,NULL),(959,'-05:00','America/Port-au-Prince','',1,'2018-07-25 09:43:56',1,NULL,NULL),(960,'-05:00','America/Resolute','',1,'2018-07-25 09:43:56',1,NULL,NULL),(961,'-05:00','America/Thunder_Bay','',1,'2018-07-25 09:43:56',1,NULL,NULL),(962,'-05:00','America/Toronto','Canada/Eastern',1,'2018-07-25 09:43:56',1,NULL,NULL),(963,'-05:00','EST','',1,'2018-07-25 09:43:56',1,NULL,NULL),(964,'-05:00','EST5EDT','',1,'2018-07-25 09:43:56',1,NULL,NULL),(965,'-05:00','Etc/GMT+5','',1,'2018-07-25 09:43:56',1,NULL,NULL),(966,'-04:30','America/Caracas','',1,'2018-07-25 09:43:56',1,NULL,NULL),(967,'-04:00','America/Anguilla','',1,'2018-07-25 09:43:56',1,NULL,NULL),(968,'-04:00','America/Antigua','',1,'2018-07-25 09:43:56',1,NULL,NULL),(969,'-03:00','America/Argentina/San_Luis','',1,'2018-07-25 09:43:56',1,NULL,NULL),(970,'-04:00','America/Aruba','',1,'2018-07-25 09:43:56',1,NULL,NULL),(971,'-04:00','America/Asuncion','',1,'2018-07-25 09:43:56',1,NULL,NULL),(972,'-04:00','America/Barbados','',1,'2018-07-25 09:43:56',1,NULL,NULL),(973,'-04:00','America/Blanc-Sablon','',1,'2018-07-25 09:43:56',1,NULL,NULL),(974,'-04:00','America/Boa_Vista','',1,'2018-07-25 09:43:56',1,NULL,NULL),(975,'-04:00','America/Campo_Grande','',1,'2018-07-25 09:43:56',1,NULL,NULL),(976,'-04:00','America/Cuiaba','',1,'2018-07-25 09:43:56',1,NULL,NULL),(977,'-04:00','America/Curacao','',1,'2018-07-25 09:43:56',1,NULL,NULL),(978,'-04:00','America/Dominica','',1,'2018-07-25 09:43:56',1,NULL,NULL),(979,'-04:00','America/Eirunepe','',1,'2018-07-25 09:43:56',1,NULL,NULL),(980,'-04:00','America/Glace_Bay','',1,'2018-07-25 09:43:56',1,NULL,NULL),(981,'-04:00','America/Goose_Bay','',1,'2018-07-25 09:43:56',1,NULL,NULL),(982,'-04:00','America/Grenada','',1,'2018-07-25 09:43:56',1,NULL,NULL),(983,'-04:00','America/Guadeloupe','America/Marigot, America/St_Barthelemy',1,'2018-07-25 09:43:56',1,NULL,NULL),(984,'-04:00','America/Guyana','',1,'2018-07-25 09:43:56',1,NULL,NULL),(985,'-04:00','America/Halifax','Canada/Atlantic',1,'2018-07-25 09:43:56',1,NULL,NULL),(986,'-04:00','America/La_Paz','',1,'2018-07-25 09:43:56',1,NULL,NULL),(987,'-04:00','America/Manaus','Brazil/West',1,'2018-07-25 09:43:56',1,NULL,NULL),(988,'-04:00','America/Martinique','',1,'2018-07-25 09:43:56',1,NULL,NULL),(989,'-04:00','America/Moncton','',1,'2018-07-25 09:43:56',1,NULL,NULL),(990,'-04:00','America/Montserrat','',1,'2018-07-25 09:43:56',1,NULL,NULL),(991,'-04:00','America/Port_of_Spain','',1,'2018-07-25 09:43:56',1,NULL,NULL),(992,'-04:00','America/Porto_Velho','',1,'2018-07-25 09:43:56',1,NULL,NULL),(993,'-04:00','America/Puerto_Rico','',1,'2018-07-25 09:43:56',1,NULL,NULL),(994,'-04:00','America/Rio_Branco','America/Porto_Acre, Brazil/Acre',1,'2018-07-25 09:43:56',1,NULL,NULL),(995,'-04:00','America/Santiago','Chile/Continental',1,'2018-07-25 09:43:56',1,NULL,NULL),(996,'-04:00','America/Santo_Domingo','',1,'2018-07-25 09:43:56',1,NULL,NULL),(997,'-04:00','America/St_Kitts','',1,'2018-07-25 09:43:56',1,NULL,NULL),(998,'-04:00','America/St_Lucia','',1,'2018-07-25 09:43:56',1,NULL,NULL),(999,'-04:00','America/St_Thomas','America/Virgin',1,'2018-07-25 09:43:56',1,NULL,NULL),(1000,'-04:00','America/St_Vincent','',1,'2018-07-25 09:43:56',1,NULL,NULL),(1001,'-04:00','America/Thule','',1,'2018-07-25 09:43:56',1,NULL,NULL),(1002,'-04:00','America/Tortola','',1,'2018-07-25 09:43:56',1,NULL,NULL),(1003,'-04:00','Antarctica/Palmer','',1,'2018-07-25 09:43:56',1,NULL,NULL),(1004,'-04:00','Atlantic/Bermuda','',1,'2018-07-25 09:43:56',1,NULL,NULL),(1005,'-04:00','Atlantic/Stanley','',1,'2018-07-25 09:43:56',1,NULL,NULL),(1006,'-04:00','Etc/GMT+4','',1,'2018-07-25 09:43:56',1,NULL,NULL),(1007,'-03:30','America/St_Johns','Canada/Newfoundland',1,'2018-07-25 09:43:56',1,NULL,NULL),(1008,'-03:00','America/Araguaina','',1,'2018-07-25 09:43:56',1,NULL,NULL),(1009,'-03:00','America/Argentina/Buenos_Aires','America/Buenos_Aires',1,'2018-07-25 09:43:56',1,NULL,NULL),(1010,'-03:00','America/Argentina/Catamarca','America/Argentina/ComodRivadavia, America/Catamarca',1,'2018-07-25 09:43:56',1,NULL,NULL),(1011,'-03:00','America/Argentina/Cordoba','America/Cordoba, America/Rosario',1,'2018-07-25 09:43:57',1,NULL,NULL),(1012,'-03:00','America/Argentina/Jujuy','America/Jujuy',1,'2018-07-25 09:43:57',1,NULL,NULL),(1013,'-03:00','America/Argentina/La_Rioja','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1014,'-03:00','America/Argentina/Mendoza','America/Mendoza',1,'2018-07-25 09:43:57',1,NULL,NULL),(1015,'-03:00','America/Argentina/Rio_Gallegos','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1016,'-03:00','America/Argentina/Salta','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1017,'-03:00','America/Argentina/San_Juan','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1018,'-03:00','America/Argentina/Tucuman','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1019,'-03:00','America/Argentina/Ushuaia','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1020,'-03:00','America/Bahia','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1021,'-03:00','America/Belem','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1022,'-03:00','America/Cayenne','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1023,'-03:00','America/Fortaleza','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1024,'-03:00','America/Godthab','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1025,'-03:00','America/Maceio','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1026,'-03:00','America/Miquelon','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1027,'-03:00','America/Montevideo','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1028,'-03:00','America/Paramaribo','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1029,'-03:00','America/Recife','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1030,'-03:00','America/Santarem','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1031,'-03:00','America/Sao_Paulo','Brazil/East',1,'2018-07-25 09:43:57',1,NULL,NULL),(1032,'-03:00','Antarctica/Rothera','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1033,'-03:00','Etc/GMT+3','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1034,'-02:00','America/Noronha','Brazil/DeNoronha',1,'2018-07-25 09:43:57',1,NULL,NULL),(1035,'-02:00','Atlantic/South_Georgia','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1036,'-02:00','Etc/GMT+2','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1037,'-01:00','America/Scoresbysund','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1038,'-01:00','Atlantic/Azores','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1039,'-01:00','Atlantic/Cape_Verde','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1040,'-01:00','Etc/GMT+1','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1041,'+00:00','Africa/Abidjan','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1042,'+00:00','Africa/Accra','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1043,'+00:00','Africa/Bamako','Africa/Timbuktu',1,'2018-07-25 09:43:57',1,NULL,NULL),(1044,'+00:00','Africa/Banjul','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1045,'+00:00','Africa/Bissau','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1046,'+00:00','Africa/Casablanca','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1047,'+00:00','Africa/Conakry','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1048,'+00:00','Africa/Dakar','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1049,'+00:00','Africa/El_Aaiun','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1050,'+00:00','Africa/Freetown','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1051,'+00:00','Africa/Lome','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1052,'+00:00','Africa/Monrovia','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1053,'+00:00','Africa/Nouakchott','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1054,'+00:00','Africa/Ouagadougou','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1055,'+00:00','Africa/Sao_Tome','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1056,'+00:00','America/Danmarkshavn','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1057,'+00:00','Atlantic/Canary','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1058,'+00:00','Atlantic/Faroe','Atlantic/Faeroe',1,'2018-07-25 09:43:57',1,NULL,NULL),(1059,'+00:00','Atlantic/Madeira','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1060,'+00:00','Atlantic/Reykjavik','Iceland',1,'2018-07-25 09:43:57',1,NULL,NULL),(1061,'+00:00','Atlantic/St_Helena','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1062,'+00:00','Etc/GMT','Etc/GMT+0, Etc/GMT-0, Etc/GMT0, Etc/Greenwich, GMT, GMT+0, GMT-0, GMT0, Greenwich',1,'2018-07-25 09:43:57',1,NULL,NULL),(1063,'+00:00','Etc/UCT','UCT',1,'2018-07-25 09:43:57',1,NULL,NULL),(1064,'+00:00','Etc/UTC','Etc/Universal, Etc/Zulu, Universal, Zulu',1,'2018-07-25 09:43:57',1,NULL,NULL),(1065,'+00:00','Europe/Dublin','Eire',1,'2018-07-25 09:43:57',1,NULL,NULL),(1066,'+00:00','Europe/Lisbon','Portugal',1,'2018-07-25 09:43:57',1,NULL,NULL),(1067,'+00:00','Europe/London','Europe/Belfast, Europe/Guernsey, Europe/Isle_of_Man, Europe/Jersey, GB, GB-Eire',1,'2018-07-25 09:43:57',1,NULL,NULL),(1068,'+00:00','UTC','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1069,'+00:00','WET','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1070,'+01:00','Africa/Algiers','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1071,'+01:00','Africa/Bangui','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1072,'+01:00','Africa/Brazzaville','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1073,'+01:00','Africa/Ceuta','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1074,'+01:00','Africa/Douala','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1075,'+01:00','Africa/Kinshasa','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1076,'+01:00','Africa/Lagos','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1077,'+01:00','Africa/Libreville','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1078,'+01:00','Africa/Luanda','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1079,'+01:00','Africa/Malabo','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1080,'+01:00','Africa/Ndjamena','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1081,'+01:00','Africa/Niamey','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1082,'+01:00','Africa/Porto-Novo','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1083,'+01:00','Africa/Tunis','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1084,'+01:00','Africa/Windhoek','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1085,'+01:00','CET','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1086,'+01:00','Etc/GMT-1','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1087,'+01:00','Europe/Amsterdam','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1088,'+01:00','Europe/Andorra','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1089,'+01:00','Europe/Belgrade','Europe/Ljubljana, Europe/Podgorica, Europe/Sarajevo, Europe/Skopje, Europe/Zagreb',1,'2018-07-25 09:43:57',1,NULL,NULL),(1090,'+01:00','Europe/Berlin','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1091,'+01:00','Europe/Brussels','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1092,'+01:00','Europe/Budapest','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1093,'+01:00','Europe/Copenhagen','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1094,'+01:00','Europe/Gibraltar','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1095,'+01:00','Europe/Luxembourg','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1096,'+01:00','Europe/Madrid','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1097,'+01:00','Europe/Malta','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1098,'+01:00','Europe/Monaco','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1099,'+01:00','Europe/Oslo','Arctic/Longyearbyen, Atlantic/Jan_Mayen',1,'2018-07-25 09:43:57',1,NULL,NULL),(1100,'+01:00','Europe/Paris','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1101,'+01:00','Europe/Prague','Europe/Bratislava',1,'2018-07-25 09:43:57',1,NULL,NULL),(1102,'+01:00','Europe/Rome','Europe/San_Marino, Europe/Vatican',1,'2018-07-25 09:43:57',1,NULL,NULL),(1103,'+01:00','Europe/Stockholm','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1104,'+01:00','Europe/Tirane','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1105,'+01:00','Europe/Vaduz','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1106,'+01:00','Europe/Vienna','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1107,'+01:00','Europe/Warsaw','Poland',1,'2018-07-25 09:43:57',1,NULL,NULL),(1108,'+01:00','Europe/Zurich','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1109,'+01:00','MET','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1110,'+02:00','Africa/Blantyre','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1111,'+02:00','Africa/Bujumbura','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1112,'+02:00','Africa/Cairo','Egypt',1,'2018-07-25 09:43:57',1,NULL,NULL),(1113,'+02:00','Africa/Gaborone','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1114,'+02:00','Africa/Harare','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1115,'+02:00','Africa/Johannesburg','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1116,'+02:00','Africa/Kigali','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1117,'+02:00','Africa/Lubumbashi','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1118,'+02:00','Africa/Lusaka','',1,'2018-07-25 09:43:57',1,NULL,NULL),(1119,'+02:00','Africa/Maputo','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1120,'+02:00','Africa/Maseru','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1121,'+02:00','Africa/Mbabane','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1122,'+02:00','Africa/Tripoli','Libya',1,'2018-07-25 09:43:58',1,NULL,NULL),(1123,'+02:00','Asia/Amman','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1124,'+02:00','Asia/Beirut','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1125,'+02:00','Asia/Damascus','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1126,'+02:00','Asia/Gaza','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1127,'+02:00','Asia/Jerusalem','Asia/Tel_Aviv, Israel',1,'2018-07-25 09:43:58',1,NULL,NULL),(1128,'+02:00','Asia/Nicosia','Europe/Nicosia',1,'2018-07-25 09:43:58',1,NULL,NULL),(1129,'+02:00','EET','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1130,'+02:00','Etc/GMT-2','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1131,'+02:00','Europe/Athens','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1132,'+02:00','Europe/Bucharest','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1133,'+02:00','Europe/Chisinau','Europe/Tiraspol',1,'2018-07-25 09:43:58',1,NULL,NULL),(1134,'+02:00','Europe/Helsinki','Europe/Mariehamn',1,'2018-07-25 09:43:58',1,NULL,NULL),(1135,'+02:00','Europe/Istanbul','Asia/Istanbul, Turkey',1,'2018-07-25 09:43:58',1,NULL,NULL),(1136,'+02:00','Europe/Kaliningrad','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1137,'+02:00','Europe/Kiev','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1138,'+02:00','Europe/Minsk','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1139,'+02:00','Europe/Riga','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1140,'+02:00','Europe/Simferopol','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1141,'+02:00','Europe/Sofia','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1142,'+02:00','Europe/Tallinn','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1143,'+02:00','Europe/Uzhgorod','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1144,'+02:00','Europe/Vilnius','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1145,'+02:00','Europe/Zaporozhye','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1146,'+03:00','Africa/Addis_Ababa','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1147,'+03:00','Africa/Asmara','Africa/Asmera',1,'2018-07-25 09:43:58',1,NULL,NULL),(1148,'+03:00','Africa/Dar_es_Salaam','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1149,'+03:00','Africa/Djibouti','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1150,'+03:00','Africa/Kampala','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1151,'+03:00','Africa/Khartoum','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1152,'+03:00','Africa/Mogadishu','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1153,'+03:00','Africa/Nairobi','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1154,'+03:00','Antarctica/Syowa','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1155,'+03:00','Asia/Aden','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1156,'+03:00','Asia/Baghdad','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1157,'+03:00','Asia/Bahrain','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1158,'+03:00','Asia/Kuwait','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1159,'+03:00','Asia/Qatar','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1160,'+03:00','Asia/Riyadh','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1161,'+03:00','Etc/GMT-3','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1162,'+03:00','Europe/Moscow','W-SU',1,'2018-07-25 09:43:58',1,NULL,NULL),(1163,'+03:00','Europe/Samara','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1164,'+03:00','Europe/Volgograd','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1165,'+03:00','Indian/Antananarivo','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1166,'+03:00','Indian/Comoro','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1167,'+03:00','Indian/Mayotte','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1168,'+03:30','Asia/Tehran','Iran',1,'2018-07-25 09:43:58',1,NULL,NULL),(1169,'+04:00','Asia/Baku','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1170,'+04:00','Asia/Dubai','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1171,'+04:00','Asia/Muscat','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1172,'+04:00','Asia/Tbilisi','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1173,'+04:00','Asia/Yerevan','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1174,'+04:00','Etc/GMT-4','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1175,'+04:00','Indian/Mahe','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1176,'+04:00','Indian/Mauritius','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1177,'+04:00','Indian/Reunion','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1178,'+04:30','Asia/Kabul','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1179,'+05:00','Antarctica/Mawson','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1180,'+05:00','Asia/Aqtau','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1181,'+05:00','Asia/Aqtobe','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1182,'+05:00','Asia/Ashgabat','Asia/Ashkhabad',1,'2018-07-25 09:43:58',1,NULL,NULL),(1183,'+05:00','Asia/Dushanbe','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1184,'+05:00','Asia/Karachi','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1185,'+05:00','Asia/Oral','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1186,'+05:00','Asia/Samarkand','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1187,'+05:00','Asia/Tashkent','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1188,'+05:00','Asia/Yekaterinburg','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1189,'+05:00','Etc/GMT-5','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1190,'+05:00','Indian/Kerguelen','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1191,'+05:00','Indian/Maldives','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1192,'+05:30','Asia/Colombo','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1193,'+05:30','Asia/Kolkata','Asia/Calcutta',1,'2018-07-25 09:43:58',1,NULL,NULL),(1194,'+05:45','Asia/Kathmandu','Asia/Katmandu',1,'2018-07-25 09:43:58',1,NULL,NULL),(1195,'+06:00','Antarctica/Vostok','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1196,'+06:00','Asia/Almaty','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1197,'+06:00','Asia/Bishkek','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1198,'+06:00','Asia/Dhaka','Asia/Dacca',1,'2018-07-25 09:43:58',1,NULL,NULL),(1199,'+06:00','Asia/Novokuznetsk','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1200,'+06:00','Asia/Novosibirsk','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1201,'+06:00','Asia/Omsk','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1202,'+06:00','Asia/Qyzylorda','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1203,'+06:00','Asia/Thimphu','Asia/Thimbu',1,'2018-07-25 09:43:58',1,NULL,NULL),(1204,'+06:00','Etc/GMT-6','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1205,'+06:00','Indian/Chagos','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1206,'+06:30','Asia/Rangoon','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1207,'+06:30','Indian/Cocos','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1208,'+07:00','Antarctica/Davis','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1209,'+07:00','Asia/Bangkok','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1210,'+07:00','Asia/Ho_Chi_Minh','Asia/Saigon',1,'2018-07-25 09:43:58',1,NULL,NULL),(1211,'+07:00','Asia/Hovd','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1212,'+07:00','Asia/Jakarta','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1213,'+07:00','Asia/Krasnoyarsk','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1214,'+07:00','Asia/Phnom_Penh','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1215,'+07:00','Asia/Pontianak','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1216,'+07:00','Asia/Vientiane','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1217,'+07:00','Etc/GMT-7','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1218,'+07:00','Indian/Christmas','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1219,'+08:00','Antarctica/Casey','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1220,'+08:00','Asia/Brunei','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1221,'+08:00','Asia/Choibalsan','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1222,'+08:00','Asia/Chongqing','Asia/Chungking',1,'2018-07-25 09:43:58',1,NULL,NULL),(1223,'+08:00','Asia/Harbin','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1224,'+08:00','Asia/Hong_Kong','Hongkong',1,'2018-07-25 09:43:58',1,NULL,NULL),(1225,'+08:00','Asia/Irkutsk','',1,'2018-07-25 09:43:58',1,NULL,NULL),(1226,'+08:00','Asia/Kashgar','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1227,'+08:00','Asia/Kuala_Lumpur','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1228,'+08:00','Asia/Kuching','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1229,'+08:00','Asia/Macau','Asia/Macao',1,'2018-07-25 09:43:59',1,NULL,NULL),(1230,'+08:00','Asia/Makassar','Asia/Ujung_Pandang',1,'2018-07-25 09:43:59',1,NULL,NULL),(1231,'+08:00','Asia/Manila','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1232,'+08:00','Asia/Shanghai','PRC',1,'2018-07-25 09:43:59',1,NULL,NULL),(1233,'+08:00','Asia/Singapore','Singapore',1,'2018-07-25 09:43:59',1,NULL,NULL),(1234,'+08:00','Asia/Taipei','ROC',1,'2018-07-25 09:43:59',1,NULL,NULL),(1235,'+08:00','Asia/Ulaanbaatar','Asia/Ulan_Bator',1,'2018-07-25 09:43:59',1,NULL,NULL),(1236,'+08:00','Asia/Urumqi','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1237,'+08:00','Australia/Perth','Australia/West',1,'2018-07-25 09:43:59',1,NULL,NULL),(1238,'+08:00','Etc/GMT-8','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1239,'+08:45','Australia/Eucla','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1240,'+09:00','Asia/Dili','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1241,'+09:00','Asia/Jayapura','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1242,'+09:00','Asia/Pyongyang','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1243,'+09:00','Asia/Seoul','ROK',1,'2018-07-25 09:43:59',1,NULL,NULL),(1244,'+09:00','Asia/Tokyo','Japan',1,'2018-07-25 09:43:59',1,NULL,NULL),(1245,'+09:00','Asia/Yakutsk','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1246,'+09:00','Etc/GMT-9','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1247,'+09:00','Pacific/Palau','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1248,'+09:30','Australia/Adelaide','Australia/South',1,'2018-07-25 09:43:59',1,NULL,NULL),(1249,'+09:30','Australia/Broken_Hill','Australia/Yancowinna',1,'2018-07-25 09:43:59',1,NULL,NULL),(1250,'+09:30','Australia/Darwin','Australia/North',1,'2018-07-25 09:43:59',1,NULL,NULL),(1251,'+10:00','Antarctica/DumontDUrville','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1252,'+10:00','Asia/Sakhalin','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1253,'+10:00','Asia/Vladivostok','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1254,'+10:00','Australia/Brisbane','Australia/Queensland',1,'2018-07-25 09:43:59',1,NULL,NULL),(1255,'+10:00','Australia/Currie','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1256,'+10:00','Australia/Hobart','Australia/Tasmania',1,'2018-07-25 09:43:59',1,NULL,NULL),(1257,'+10:00','Australia/Lindeman','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1258,'+10:00','Australia/Melbourne','Australia/Victoria',1,'2018-07-25 09:43:59',1,NULL,NULL),(1259,'+10:00','Australia/Sydney','Australia/ACT, Australia/Canberra, Australia/NSW',1,'2018-07-25 09:43:59',1,NULL,NULL),(1260,'+10:00','Etc/GMT-10','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1261,'+10:00','Pacific/Chuuk','Pacific/Truk, Pacific/Yap',1,'2018-07-25 09:43:59',1,NULL,NULL),(1262,'+10:00','Pacific/Guam','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1263,'+10:00','Pacific/Port_Moresby','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1264,'+10:00','Pacific/Saipan','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1265,'+10:30','Australia/Lord_Howe','Australia/LHI',1,'2018-07-25 09:43:59',1,NULL,NULL),(1266,'+11:00','Antarctica/Macquarie','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1267,'+11:00','Asia/Anadyr','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1268,'+11:00','Asia/Kamchatka','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1269,'+11:00','Asia/Magadan','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1270,'+11:00','Etc/GMT-11','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1271,'+11:00','Pacific/Efate','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1272,'+11:00','Pacific/Guadalcanal','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1273,'+11:00','Pacific/Kosrae','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1274,'+11:00','Pacific/Noumea','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1275,'+11:00','Pacific/Pohnpei','Pacific/Ponape',1,'2018-07-25 09:43:59',1,NULL,NULL),(1276,'+11:30','Pacific/Norfolk','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1277,'+12:00','Antarctica/McMurdo','Antarctica/South_Pole',1,'2018-07-25 09:43:59',1,NULL,NULL),(1278,'+12:00','Etc/GMT-12','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1279,'+12:00','Pacific/Auckland','NZ',1,'2018-07-25 09:43:59',1,NULL,NULL),(1280,'+12:00','Pacific/Fiji','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1281,'+12:00','Pacific/Funafuti','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1282,'+12:00','Pacific/Kwajalein','Kwajalein',1,'2018-07-25 09:43:59',1,NULL,NULL),(1283,'+12:00','Pacific/Majuro','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1284,'+12:00','Pacific/Nauru','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1285,'+12:00','Pacific/Tarawa','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1286,'+12:00','Pacific/Wake','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1287,'+12:00','Pacific/Wallis','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1288,'+12:45','Pacific/Chatham','NZ-CHAT',1,'2018-07-25 09:43:59',1,NULL,NULL),(1289,'+13:00','Etc/GMT-13','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1290,'+13:00','Pacific/Enderbury','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1291,'+13:00','Pacific/Tongatapu','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1292,'+14:00','Etc/GMT-14','',1,'2018-07-25 09:43:59',1,NULL,NULL),(1293,'+14:00','Pacific/Kiritimati','',1,'2018-07-25 09:44:06',1,NULL,NULL);
/*!40000 ALTER TABLE `time_zone` ENABLE KEYS */;
UNLOCK TABLES;
--
-- Table structure for table `type_service`
--

DROP TABLE IF EXISTS `type_service`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `type_service` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `description` varchar(255) NOT NULL,
  `controller` varchar(50) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `type_vehicle`
--

DROP TABLE IF EXISTS `type_vehicle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `type_vehicle` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `description` text,
  `work_type` enum('0','1','2') NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `vehicle`
--

DROP TABLE IF EXISTS `vehicle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vehicle` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` varchar(254) DEFAULT NULL,
  `alias` varchar(50) DEFAULT NULL,
  `serial_num` varchar(50) NOT NULL,
  `model` varchar(100) NOT NULL,
  `vehicle_year` int(4) NOT NULL,
  `plate` varchar(10) DEFAULT NULL,
  `fuel_capacity` decimal(8,2) NOT NULL DEFAULT '0.00',
  `km_liters` decimal(8,2) NOT NULL DEFAULT '0.00',
  `odomether` int(11) NOT NULL DEFAULT '0',
  `config_vehicle_id` int(11) DEFAULT NULL,
  `type_vehicle_id` int(11) DEFAULT NULL,
  `policy` varchar(50) DEFAULT NULL,
  `policy_date` date DEFAULT NULL,
  `last_maintenance` date DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  `time_checkpoint` int(11) DEFAULT NULL,
  `time_manteinance` int(11) DEFAULT NULL,
  `img_file` varchar(255) DEFAULT NULL,
  `economic_number` varchar(50) NOT NULL,
  `brand` varchar(100) NOT NULL,
  `plate_state` varchar(10) DEFAULT NULL,
  `sct_license` varchar(50) DEFAULT NULL,
  `policy_insurance` varchar(150) DEFAULT NULL,
  `circulation_card_date` date DEFAULT NULL,
  `branchoffice_id` int(11) DEFAULT NULL,
  `info_completed` tinyint(1) NOT NULL DEFAULT '0',
  `work_type` enum('0','1','2') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `config_vehicle_id` (`config_vehicle_id`),
  KEY `fk_vehicle_branchoffice` (`branchoffice_id`),
  CONSTRAINT `fk_vehicle_branchoffice` FOREIGN KEY (`branchoffice_id`) REFERENCES `branchoffice` (`id`),
  CONSTRAINT `vehicle_ibfk_1` FOREIGN KEY (`config_vehicle_id`) REFERENCES `config_vehicle` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vehicle`
--

LOCK TABLES `vehicle` WRITE;
/*!40000 ALTER TABLE `vehicle` DISABLE KEYS */;
/*!40000 ALTER TABLE `vehicle` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vehicle_casefile`
--

DROP TABLE IF EXISTS `vehicle_casefile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vehicle_casefile` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `vehicle_id` int(11) NOT NULL,
  `casefile_id` int(11) NOT NULL,
  `file` text,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_vehicle_id_casefile` (`vehicle_id`),
  KEY `fk_casefile_id_casefile` (`casefile_id`),
  CONSTRAINT `fk_casefile_id_casefile` FOREIGN KEY (`casefile_id`) REFERENCES `casefile` (`id`),
  CONSTRAINT `fk_vehicle_id_casefile` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vehicle_casefile`
--

LOCK TABLES `vehicle_casefile` WRITE;
/*!40000 ALTER TABLE `vehicle_casefile` DISABLE KEYS */;
/*!40000 ALTER TABLE `vehicle_casefile` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vehicle_characteristic`
--

DROP TABLE IF EXISTS `vehicle_characteristic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vehicle_characteristic` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `vehicle_id` int(11) NOT NULL,
  `characteristic_id` int(11) NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_vehicle_id_charact` (`vehicle_id`),
  KEY `fk_characteristic_id_charact` (`characteristic_id`),
  CONSTRAINT `fk_characteristic_id_charact` FOREIGN KEY (`characteristic_id`) REFERENCES `characteristic` (`id`),
  CONSTRAINT `fk_vehicle_id_charact` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;



--
-- Table structure for table `vehicles_addons`
--

DROP TABLE IF EXISTS `vehicles_addons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vehicles_addons` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `addon_vehicle_id` int(11) NOT NULL,
  `vehicle_id` int(11) NOT NULL,
  `addon_serial_id` int(11) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` int(11) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `updated_by` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_vehicles_addons_addon_vehicle` (`addon_vehicle_id`),
  KEY `fk_vehicles_addons_vehicle` (`vehicle_id`),
  KEY `fk_vehicles_addons_addon_serial` (`addon_serial_id`),
  CONSTRAINT `fk_vehicles_addons_addon_serial` FOREIGN KEY (`addon_serial_id`) REFERENCES `addon_serial` (`id`),
  CONSTRAINT `fk_vehicles_addons_addon_vehicle` FOREIGN KEY (`addon_vehicle_id`) REFERENCES `addon_vehicle` (`id`),
  CONSTRAINT `fk_vehicles_addons_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2018-10-25 12:20:06
