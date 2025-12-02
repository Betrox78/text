-- 1568330741 UP procedure commercial promise report by terminal
DROP PROCEDURE IF EXISTS PROCEDURE_COMMERCIAL_PROMISE_REPORT_BY_TERMINAL;
DELIMITER $$

CREATE DEFINER = CURRENT_USER PROCEDURE PROCEDURE_COMMERCIAL_PROMISE_REPORT_BY_TERMINAL(IN DATE_CURRENT DATE, IN ID_TERMINAL INT, IN TIME_ZONE VARCHAR(6))

BEGIN

DECLARE TOTAL_DAYS_OF_MONTH INT DEFAULT 0;
DECLARE CURRENT_DAY INT DEFAULT 1;
DECLARE finished_percent INT DEFAULT 0;
DECLARE unfinished_percent INT DEFAULT 0;
DECLARE parcels_total INT DEFAULT 0;
DECLARE parcels_finished INT DEFAULT 0;
DECLARE parcels_unfinished INT DEFAULT 0;

SELECT SUBDATE(DATE_CURRENT, DAYOFMONTH(DATE_CURRENT) - 1) INTO DATE_CURRENT;
SELECT DAY(LAST_DAY(DATE_CURRENT)) INTO TOTAL_DAYS_OF_MONTH;

   CREATE TEMPORARY TABLE REPORT(
	number_day INT(2) DEFAULT 1,
    parcels_total INT(11) DEFAULT 0,
    parcels_finished INT(11) DEFAULT 0,
    parcels_unfinished INT(11) DEFAULT 0,
    finished_percent DECIMAL(12, 2) DEFAULT 0,
    unfinished_percent DECIMAL(12, 2) DEFAULT 0
   );

    -- CICLO RECORRE LOS DIAS DEL MES
	WHILE(CURRENT_DAY <= TOTAL_DAYS_OF_MONTH) DO

		-- INICIA CICLO DIAS
		-- TOTAL DE PARCELS EN UN MES ESPEFICICO ENTREGADOS E INCONCLUSOS
		SELECT COUNT(p.id) FROM parcels p
		WHERE p.parcel_status IN (2, 3)
		AND DATE(CONVERT_TZ(p.delivered_at, '+00:00', TIME_ZONE)) = DATE_CURRENT
		AND p.terminal_destiny_id = ID_TERMINAL INTO parcels_total;

		-- TOTAL DE PARCELS DE UN MES ESPECIFICO CON LA TOTALIDAD DE PAQUETES ENTREGADOS OK
		SELECT COUNT(p.id) FROM parcels p
		WHERE (p.parcel_status IN (2, 3)
		AND p.is_internal_parcel IS FALSE
		AND (p.delivered_at IS NOT NULL AND CONVERT_TZ(p.delivered_at, '+00:00', TIME_ZONE) <= CONVERT_TZ(p.promise_delivery_date, '+00:00', TIME_ZONE)))
		AND DATE(CONVERT_TZ(p.delivered_at, '+00:00', TIME_ZONE)) = DATE_CURRENT
		AND p.terminal_destiny_id = ID_TERMINAL INTO parcels_finished;

		-- TOTAL DE PARCELS DE UN MES ESPECIFICO CON LA TOTALIDAD DE PAQUETES INCONCLUSOS
		SELECT COUNT(p.id) FROM parcels p
		WHERE (p.parcel_status IN (2, 3)
		AND (p.delivered_at IS NOT NULL AND CONVERT_TZ(p.delivered_at, '+00:00', TIME_ZONE) > CONVERT_TZ(p.promise_delivery_date, '+00:00', TIME_ZONE)))
		AND DATE(CONVERT_TZ(p.delivered_at, '+00:00', TIME_ZONE)) = DATE_CURRENT
		AND p.terminal_destiny_id = ID_TERMINAL INTO parcels_unfinished;

		SELECT IF(parcels_total != 0, (parcels_finished * 100) / parcels_total, 100) INTO finished_percent;
		SELECT IF(parcels_total != 0, 100 - finished_percent, 0) INTO unfinished_percent;

		INSERT INTO REPORT(number_day, parcels_total, parcels_finished, parcels_unfinished, finished_percent, unfinished_percent)
		VALUES(CURRENT_DAY, parcels_total, parcels_finished, parcels_unfinished, finished_percent, unfinished_percent);


		SET CURRENT_DAY = CURRENT_DAY + 1;
		SELECT ADDDATE(DATE_CURRENT, INTERVAL 1 DAY) INTO DATE_CURRENT;
		-- TERMINA CICLO DIAS

	END WHILE;

    SELECT * FROM REPORT;
	DROP TEMPORARY TABLE REPORT;

END $$

DELIMITER ;

GRANT EXECUTE ON PROCEDURE PROCEDURE_COMMERCIAL_PROMISE_REPORT_BY_TERMINAL TO 'abordo'@'%';