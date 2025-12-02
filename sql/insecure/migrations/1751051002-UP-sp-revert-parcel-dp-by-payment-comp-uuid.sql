-- 1751051002 UP sp-revert-parcel-dp-by-payment-comp-uuid
DROP PROCEDURE IF EXISTS `revert_parcels_debt_payments_by_payment_complement_uuid`;
DELIMITER $$

CREATE DEFINER = CURRENT_USER PROCEDURE `revert_parcels_debt_payments_by_payment_complement_uuid`(IN complement_uuid_input VARCHAR(50))
BEGIN
  -- 0) Declaración de variables (todas antes de cualquier sentencia)
  DECLARE v_pc_id                       INT;
  DECLARE v_payment_id                  INT;
  DECLARE v_ticket_id                   INT;
  DECLARE v_ticket_code                 VARCHAR(60);
  DECLARE v_customer_id                 INT;
  DECLARE v_credit_original             DECIMAL(12,2);
  DECLARE v_credit_final                DECIMAL(12,2);
  DECLARE v_total_debt_original         DECIMAL(12,2) DEFAULT 0.00;
  DECLARE v_total_debt_final            DECIMAL(12,2) DEFAULT 0.00;
  DECLARE done_payments                 INT DEFAULT FALSE;
  DECLARE v_error_msg                   VARCHAR(255);

  -- Cursor para payments vinculados al complemento
  DECLARE pc_cursor CURSOR FOR
    SELECT payment_id
      FROM payment_complement_payment
     WHERE payment_complement_id = v_pc_id;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done_payments = TRUE;

  -- 1) Resolvemos el id del complemento
  SELECT id
    INTO v_pc_id
    FROM payment_complement
   WHERE uuid = complement_uuid_input
   LIMIT 1;
  IF v_pc_id IS NULL THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'No se encontró complemento de pago con ese UUID';
  END IF;

  -- 2) Iteramos cada payment de ese complemento
  OPEN pc_cursor;
  pc_loop: LOOP
    FETCH pc_cursor INTO v_payment_id;
    IF done_payments THEN
      LEAVE pc_loop;
    END IF;

    -- 2a) Obtenemos ticket, código y customer
    SELECT t.id, t.ticket_code, dp.customer_id
      INTO v_ticket_id, v_ticket_code, v_customer_id
      FROM payment p
      JOIN tickets t     ON t.id = p.ticket_id
      JOIN debt_payment dp ON dp.ticket_id = t.id
     WHERE p.id = v_payment_id
     LIMIT 1;
    IF v_ticket_id IS NULL THEN
      SET v_error_msg = CONCAT('Pago ', v_payment_id, ' sin ticket');
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = v_error_msg;
    END IF;

    -- 2b) Guardamos el crédito original y reseteamos totales
    SELECT credit_available
      INTO v_credit_original
      FROM customer
     WHERE id = v_customer_id;
    SET v_total_debt_original = 0.00,
        v_total_debt_final    = 0.00;

    -- 2c) Bloque anidado: recorremos debt_payment de este ticket
    BEGIN
      -- Declaraciones del bloque (antes de cualquier sentencia)
      DECLARE done_debts                    INT DEFAULT FALSE;
      DECLARE v_debt_payment_id             INT;
      DECLARE v_parcel_id                   INT;
      DECLARE v_amount                      DECIMAL(12,2);
      DECLARE v_payment_complement_id_inner INT;
      DECLARE v_invoice_id                  INT;
      DECLARE v_debt_original               DECIMAL(12,2);
      DECLARE v_debt_final                  DECIMAL(12,2);

      DECLARE dp_cursor CURSOR FOR
        SELECT id, parcel_id, customer_id, amount, payment_complement_id
          FROM debt_payment
         WHERE ticket_id = v_ticket_id;
      DECLARE CONTINUE HANDLER FOR NOT FOUND SET done_debts = TRUE;

      OPEN dp_cursor;
      dp_loop: LOOP
        FETCH dp_cursor
          INTO v_debt_payment_id, v_parcel_id, v_customer_id, v_amount, v_payment_complement_id_inner;
        IF done_debts THEN
          LEAVE dp_loop;
        END IF;

        -- 1) Deuda original del envío
        SELECT debt
          INTO v_debt_original
          FROM parcels
         WHERE id = v_parcel_id;
        SET v_total_debt_original = v_total_debt_original + v_debt_original;

        -- 2) Revertir deuda en parcels
        UPDATE parcels
           SET debt = debt + v_amount
         WHERE id = v_parcel_id;

        -- 3) Revertir crédito del cliente
        UPDATE customer
           SET credit_available = credit_available - v_amount
         WHERE id = v_customer_id;

        -- 4) Deuda final del envío
        SELECT debt
          INTO v_debt_final
          FROM parcels
         WHERE id = v_parcel_id;
        SET v_total_debt_final = v_total_debt_final + v_debt_final;

        -- 5) Si existe payment_complement asociado, revertimos detalles
        IF v_payment_complement_id_inner IS NOT NULL THEN
          -- 5a) Restaurar available_amount_for_complement en factura
          SELECT invoice_id
            INTO v_invoice_id
            FROM parcels
           WHERE id = v_parcel_id
           LIMIT 1;
          IF v_invoice_id IS NOT NULL THEN
            UPDATE invoice
               SET available_amount_for_complement = available_amount_for_complement + v_amount,
                   updated_by                    = 1,
                   updated_at                    = NOW()
             WHERE id = v_invoice_id;
          END IF;

          -- 5b) Eliminar detalle de complemento
          DELETE FROM payment_complement_detail
           WHERE payment_complement_id = v_payment_complement_id_inner
             AND invoice_id            = v_invoice_id;

          -- 5c) Si ya no quedan detalles, limpiamos y borramos registros del complemento
          IF (SELECT COUNT(*)
                FROM payment_complement_detail
               WHERE payment_complement_id = v_payment_complement_id_inner
             ) = 0 THEN
            UPDATE debt_payment
               SET payment_complement_id = NULL
             WHERE payment_complement_id = v_payment_complement_id_inner;

            DELETE FROM payment_complement_invoice
             WHERE payment_complement_id = v_payment_complement_id_inner;
            DELETE FROM payment_complement_payment
             WHERE payment_complement_id = v_payment_complement_id_inner;
            DELETE FROM payment_complement
             WHERE id                    = v_payment_complement_id_inner;
          END IF;
        END IF;

        -- 6) Eliminar el debt_payment
        DELETE FROM debt_payment
         WHERE id = v_debt_payment_id;
      END LOOP dp_loop;
      CLOSE dp_cursor;
    END;

    -- 2d) Capturamos crédito final
    SELECT credit_available
      INTO v_credit_final
      FROM customer
     WHERE id = v_customer_id;

    -- 2e) Limpiamos todo lo asociado al ticket
    DELETE FROM tickets_details        WHERE ticket_id = v_ticket_id;
    DELETE FROM cash_out_move          WHERE payment_id  = v_payment_id;
    DELETE FROM payment_complement_payment
     WHERE payment_complement_id = v_pc_id
       AND payment_id            = v_payment_id;
    DELETE FROM payment              WHERE id = v_payment_id;
    DELETE FROM tickets              WHERE id = v_ticket_id;

    -- 2f) Devolvemos resumen de este pago
    SELECT
      v_ticket_code          AS ticket,
      v_ticket_id            AS ticket_id,
      v_payment_id           AS payment_id,
      v_customer_id          AS customer_id,
      v_total_debt_original  AS adeudo,
      v_total_debt_final     AS nuevo_adeudo,
      v_credit_original      AS credito_disponible,
      v_credit_final         AS nuevo_credito_disponible;
  END LOOP pc_loop;
  CLOSE pc_cursor;

  -- 3) Una vez procesados todos los pagos, limpiamos el complemento
  UPDATE debt_payment
     SET payment_complement_id = NULL
   WHERE payment_complement_id = v_pc_id;

  DELETE FROM payment_complement_detail   WHERE payment_complement_id = v_pc_id;
  DELETE FROM payment_complement_invoice  WHERE payment_complement_id = v_pc_id;
  DELETE FROM payment_complement_payment  WHERE payment_complement_id = v_pc_id;
  DELETE FROM payment_complement          WHERE id                    = v_pc_id;
END $$
DELIMITER ;

GRANT EXECUTE ON PROCEDURE revert_parcels_prepaid_debt_payment_by_ticket_code TO 'abordo'@'%';