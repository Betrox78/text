-- 1751031837 UP new-parcels-prepaid-debt-procedure
DROP PROCEDURE IF EXISTS revert_parcels_prepaid_debt_payment_by_ticket_code;
DELIMITER $$

CREATE DEFINER = CURRENT_USER PROCEDURE revert_parcels_prepaid_debt_payment_by_ticket_code(IN ticket_code_input VARCHAR(60))
BEGIN
    DECLARE v_ticket_id INT;
    DECLARE v_payment_id INT;
    DECLARE v_debt_payment_id INT;
    DECLARE v_parcel_prepaid_id INT;
    DECLARE v_customer_id INT;
    DECLARE v_amount DECIMAL(12,2);
    DECLARE v_payment_complement_id INT;
    DECLARE v_invoice_id INT;
    DECLARE v_debt_original DECIMAL(12,2);
    DECLARE v_debt_final DECIMAL(12,2);
    DECLARE done INT DEFAULT FALSE;

    DECLARE v_total_debt_original DECIMAL(12,2) DEFAULT 0.00;
    DECLARE v_total_debt_final DECIMAL(12,2) DEFAULT 0.00;
    DECLARE v_credit_original DECIMAL(12,2) DEFAULT 0.00;
    DECLARE v_credit_final DECIMAL(12,2) DEFAULT 0.00;

    DECLARE dp_cursor CURSOR FOR
        SELECT id, parcel_prepaid_id, customer_id, amount, payment_complement_id
        FROM debt_payment
        WHERE ticket_id = v_ticket_id;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- Obtener ticket_id
    SELECT id INTO v_ticket_id
    FROM tickets
    WHERE ticket_code = ticket_code_input
    LIMIT 1;

    IF v_ticket_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ticket no encontrado';
    END IF;

    -- tiene complemento de pago multi cliente
    IF EXISTS (
      SELECT 1
        FROM payment p
        JOIN payment_complement_payment pcp
          ON pcp.payment_id = p.id
        JOIN payment_complement pc
          ON pc.id = pcp.payment_complement_id
       WHERE p.ticket_id = v_ticket_id
         AND pc.is_multi_customer = 1
    ) THEN
        SIGNAL SQLSTATE '45000'
          SET MESSAGE_TEXT = 'No se puede revertir un abono con complemento multi‐cliente. Use el procedimiento específico.';
    END IF;

    -- Obtener payment_id
    SELECT id INTO v_payment_id
    FROM payment
    WHERE ticket_id = v_ticket_id
    LIMIT 1;

    -- Obtener customer_id
    SELECT customer_id INTO v_customer_id
    FROM debt_payment
    WHERE ticket_id = v_ticket_id
    LIMIT 1;

    -- Crédito original
    SELECT credit_available INTO v_credit_original
    FROM customer
    WHERE id = v_customer_id;

    -- Procesar cada debt_payment del ticket
    OPEN dp_cursor;
    dp_loop: LOOP
        FETCH dp_cursor INTO v_debt_payment_id, v_parcel_prepaid_id, v_customer_id, v_amount, v_payment_complement_id;
        IF done THEN
            LEAVE dp_loop;
        END IF;

        -- Deuda original
        SELECT debt INTO v_debt_original FROM parcels_prepaid WHERE id = v_parcel_prepaid_id;
        SET v_total_debt_original = v_total_debt_original + v_debt_original;

        -- Revertir deuda
        UPDATE parcels_prepaid
        SET debt = debt + v_amount
        WHERE id = v_parcel_prepaid_id;

        -- Revertir crédito
        UPDATE customer
        SET credit_available = credit_available - v_amount
        WHERE id = v_customer_id;

        -- Nueva deuda
        SELECT debt INTO v_debt_final FROM parcels_prepaid WHERE id = v_parcel_prepaid_id;
        SET v_total_debt_final = v_total_debt_final + v_debt_final;

        -- Manejar complemento de pago
        IF v_payment_complement_id IS NOT NULL THEN
            SELECT invoice_id INTO v_invoice_id FROM parcels_prepaid WHERE id = v_parcel_prepaid_id LIMIT 1;

            IF v_invoice_id IS NOT NULL THEN
                UPDATE invoice
                SET available_amount_for_complement = available_amount_for_complement + v_amount,
                    updated_by = 1,
                    updated_at = NOW()
                WHERE id = v_invoice_id;
            END IF;

            DELETE FROM payment_complement_detail
            WHERE payment_complement_id = v_payment_complement_id
              AND invoice_id = v_invoice_id;

            IF (SELECT COUNT(*) FROM payment_complement_detail WHERE payment_complement_id = v_payment_complement_id) = 0 THEN
                UPDATE debt_payment
                SET payment_complement_id = NULL
                WHERE payment_complement_id = v_payment_complement_id;

                DELETE FROM payment_complement_invoice WHERE payment_complement_id = v_payment_complement_id;
                DELETE FROM payment_complement_payment WHERE payment_complement_id = v_payment_complement_id;
                DELETE FROM payment_complement WHERE id = v_payment_complement_id;
            END IF;
        END IF;

        DELETE FROM debt_payment WHERE id = v_debt_payment_id;
    END LOOP;
    CLOSE dp_cursor;

    -- Eliminar relacionados al ticket
    DELETE FROM tickets_details WHERE ticket_id = v_ticket_id;
    DELETE FROM cash_out_move WHERE payment_id = v_payment_id;
    DELETE FROM payment WHERE id = v_payment_id;
    DELETE FROM tickets WHERE id = v_ticket_id;

    -- Crédito final
    SELECT credit_available INTO v_credit_final
    FROM customer
    WHERE id = v_customer_id;

    -- Resumen
    SELECT
        ticket_code_input AS `ticket`,
        v_ticket_id AS `ticket_id`,
        v_payment_id AS `payment_id`,
        v_customer_id AS `customer_id`,
        v_total_debt_original AS `adeudo`,
        v_total_debt_final AS `nuevo_adeudo`,
        v_credit_original AS `credito_disponible`,
        v_credit_final AS `nuevo_credito_disponible`;
END $$

DELIMITER ;

GRANT EXECUTE ON PROCEDURE revert_parcels_prepaid_debt_payment_by_ticket_code TO 'abordo'@'%';