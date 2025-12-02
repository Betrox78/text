-- 1568330770 UP procedure global demeanor commercial promise report
DROP PROCEDURE IF EXISTS PROCEDURE_GLOBAL_DEMEANOR_COMMERCIAL_PROMISE_REPORT;
DELIMITER $$

CREATE DEFINER = CURRENT_USER PROCEDURE PROCEDURE_GLOBAL_DEMEANOR_COMMERCIAL_PROMISE_REPORT(IN INIT_DATE DATE, IN END_DATE DATE, IN TERMINAL_DESTINY_ID INT, IN TIMEZONE_VALUE VARCHAR(6))

BEGIN

DECLARE parcels_total INT DEFAULT 0;
DECLARE parcels_finished INT DEFAULT 0;
DECLARE parcels_unfinished INT DEFAULT 0;
DECLARE finished_percent DECIMAL(12, 2) DEFAULT 0;

DECLARE FINISHED_DELIVERY_DATE DATETIME;
DECLARE UNFINISHED_DELIVERY_DATE DATETIME;
DECLARE TERMINAL_ORIGIN_ID INT(11);
DECLARE TERMINAL_ORIGIN_PREFIX VARCHAR(10);
DECLARE SERVER_TIMEZONE_VALUE VARCHAR(6) DEFAULT '+00:00';
DECLARE FLAG_LOOP_TERMINALS_ORIGIN INT DEFAULT FALSE;
DECLARE TERMINALS_ORIGIN_CURSOR CURSOR FOR
SELECT id, prefix FROM branchoffice WHERE status = 1 AND branch_office_type = 'T' AND id != TERMINAL_DESTINY_ID;
DECLARE CONTINUE HANDLER FOR NOT FOUND SET FLAG_LOOP_TERMINALS_ORIGIN = TRUE;

    CREATE TEMPORARY TABLE GLOBAL_REPORT(
        terminal_origin_id INT(11),
        terminal_origin_prefix VARCHAR(10),
        parcels_total INT(11) DEFAULT 0,
        parcels_finished INT(11) DEFAULT 0,
        parcels_unfinished INT(11) DEFAULT 0,
        finished_percent DECIMAL(12, 2) DEFAULT 0.0
   );

    OPEN TERMINALS_ORIGIN_CURSOR;

        -- CICLO RECORRE TERMINALES
        TERMINALS_ORIGIN_LOOP: LOOP

            FETCH TERMINALS_ORIGIN_CURSOR INTO TERMINAL_ORIGIN_ID, TERMINAL_ORIGIN_PREFIX;

            -- CIERRA CICLO DE TERMINALES
            IF FLAG_LOOP_TERMINALS_ORIGIN THEN LEAVE TERMINALS_ORIGIN_LOOP; END IF;

            -- INICIA CICLO DE TERMINALES

            -- TOTAL DE ENTREGADOS
            SELECT COUNT(p.id) FROM parcels p
            WHERE p.parcel_status IN (2, 3)
            AND p.is_internal_parcel IS FALSE
            AND DATE(CONVERT_TZ(p.delivered_at, SERVER_TIMEZONE_VALUE, TIMEZONE_VALUE)) BETWEEN INIT_DATE AND END_DATE
            AND p.terminal_origin_id = TERMINAL_ORIGIN_ID
            AND p.terminal_destiny_id = TERMINAL_DESTINY_ID INTO parcels_total;

            -- TOTAL DE PARCELS DE UN MES ESPECIFICO CON LA TOTALIDAD DE PAQUETES ENTREGADOS OK
            SELECT COUNT(p.id) FROM parcels p
            WHERE p.parcel_status IN (2, 3)
            AND p.is_internal_parcel IS FALSE
			AND p.delivered_at <= p.promise_delivery_date
            AND DATE(CONVERT_TZ(p.delivered_at, SERVER_TIMEZONE_VALUE, TIMEZONE_VALUE)) BETWEEN INIT_DATE AND END_DATE
            AND p.terminal_origin_id = TERMINAL_ORIGIN_ID
            AND p.terminal_destiny_id = TERMINAL_DESTINY_ID INTO parcels_finished;

            -- TOTAL DE PARCELS DE UN MES ESPECIFICO CON LA TOTALIDAD DE PAQUETES INCONCLUSOS
            SELECT COUNT(p.id) FROM parcels p
            WHERE p.parcel_status IN (2, 3)
            AND p.is_internal_parcel IS FALSE
			AND p.delivered_at > p.promise_delivery_date
            AND DATE(CONVERT_TZ(p.delivered_at, SERVER_TIMEZONE_VALUE, TIMEZONE_VALUE)) BETWEEN INIT_DATE AND END_DATE
            AND p.terminal_origin_id = TERMINAL_ORIGIN_ID
            AND p.terminal_destiny_id = TERMINAL_DESTINY_ID INTO parcels_unfinished;

            SELECT IF(parcels_total != 0, (parcels_finished * 100) / parcels_total, 0) INTO finished_percent;

            INSERT INTO GLOBAL_REPORT(terminal_origin_id, terminal_origin_prefix, parcels_total, parcels_finished, parcels_unfinished, finished_percent)
            VALUES(TERMINAL_ORIGIN_ID, TERMINAL_ORIGIN_PREFIX, parcels_total, parcels_finished, parcels_unfinished, finished_percent);

            -- TERMINAL CICLO DE TERMINALES

        END LOOP;

    CLOSE TERMINALS_ORIGIN_CURSOR;

    SELECT * FROM GLOBAL_REPORT;
    DROP TEMPORARY TABLE GLOBAL_REPORT;

END $$

DELIMITER ;

GRANT EXECUTE ON PROCEDURE PROCEDURE_GLOBAL_DEMEANOR_COMMERCIAL_PROMISE_REPORT TO 'abordo'@'%';