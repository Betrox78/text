-- 1574992816 UP procedure create schedule route code
DROP PROCEDURE IF EXISTS PROCEDURE_CREATE_SCHEDULE_ROUTE_CODE;
DELIMITER $$
CREATE DEFINER = CURRENT_USER PROCEDURE PROCEDURE_CREATE_SCHEDULE_ROUTE_CODE()
BEGIN
DECLARE fetch_travel_date VARCHAR(10);
DECLARE flag_loop_travel_date BOOLEAN DEFAULT FALSE;
DECLARE travel_date_cursor CURSOR FOR SELECT DATE(travel_date) AS TRAVEL_DATE FROM schedule_route WHERE status = 1 GROUP BY DATE(travel_date);
DECLARE CONTINUE HANDLER FOR NOT FOUND SET flag_loop_travel_date = TRUE;

    CREATE TEMPORARY TABLE schedule_route_codes(
        id INT(11),
        code VARCHAR(50)
   );

    BEGIN
		OPEN travel_date_cursor;
			travel_date_loop: LOOP
				FETCH travel_date_cursor INTO fetch_travel_date;
				IF flag_loop_travel_date THEN LEAVE travel_date_loop; END IF;

                BEGIN
					DECLARE fetch_config_route_id INT(11);
					DECLARE flag_loop_config_route BOOLEAN DEFAULT FALSE;
					DECLARE config_route_cursor CURSOR FOR
						SELECT DISTINCT cr.id AS config_route_id FROM config_route cr
						INNER JOIN schedule_route sr ON sr.config_route_id = cr.id
						WHERE DATE(sr.travel_date) = fetch_travel_date;
					DECLARE CONTINUE HANDLER FOR NOT FOUND SET flag_loop_config_route = TRUE;

					OPEN config_route_cursor;
						config_route_loop: LOOP
							FETCH config_route_cursor INTO fetch_config_route_id;
							IF flag_loop_config_route THEN LEAVE config_route_loop; END IF;

							INSERT INTO schedule_route_codes
                            SELECT
								a.id,
                                CONCAT(LPAD(a.config_route_id, 3, '0'), '-', DATE_FORMAT(a.travel_date, '%d%m%Y'), '-', @rownum := @rownum + 1) AS code
							FROM (SELECT
									sr.id,
									cs.travel_hour,
                                    sr.travel_date,
                                    sr.config_route_id
								FROM schedule_route sr
								LEFT JOIN config_schedule cs ON cs.id = sr.config_schedule_id
								WHERE DATE(sr.travel_date) = fetch_travel_date
								AND sr.config_route_id = fetch_config_route_id) AS a,
							(SELECT @rownum := 0) r
							ORDER BY a.travel_hour;

						END LOOP;
					CLOSE config_route_cursor;
				END;

			END LOOP;
		CLOSE travel_date_cursor;
    END;

    BEGIN
		DECLARE fetch_id INT(11);
        DECLARE fetch_code VARCHAR(50);
		DECLARE flag_loop_codes BOOLEAN DEFAULT FALSE;
		DECLARE codes_cursor CURSOR FOR SELECT id, code FROM schedule_route_codes;
		DECLARE CONTINUE HANDLER FOR NOT FOUND SET flag_loop_codes = TRUE;

        OPEN codes_cursor;
			codes_loop: LOOP
				FETCH codes_cursor INTO fetch_id, fetch_code;
                IF flag_loop_codes THEN LEAVE codes_loop; END IF;

                UPDATE schedule_route SET code = fetch_code WHERE id = fetch_id;

			END LOOP;
		CLOSE codes_cursor;

    END;

    DROP TEMPORARY TABLE schedule_route_codes;

END $$
DELIMITER ;

GRANT EXECUTE ON PROCEDURE PROCEDURE_CREATE_SCHEDULE_ROUTE_CODE TO 'abordo'@'%';