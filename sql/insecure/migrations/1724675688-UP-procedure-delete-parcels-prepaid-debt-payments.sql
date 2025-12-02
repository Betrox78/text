-- 1724675688 UP procedure-delete-parcels-prepaid-debt-payments
DROP PROCEDURE IF EXISTS revert_parcels_prepaid_debt_payment;
DELIMITER $$

CREATE DEFINER = CURRENT_USER PROCEDURE revert_parcels_prepaid_debt_payment(IN tracking_code_input VARCHAR(20))
BEGIN
    -- Declaración de variables locales
    DECLARE prepaid_id INT;
    DECLARE original_debt DECIMAL(12,2);
    DECLARE original_credit DECIMAL(12,2);
    DECLARE final_debt DECIMAL(12,2);
    DECLARE final_credit DECIMAL(12,2);
    DECLARE remaining_details INT;
    DECLARE cash_out_move_exists INT DEFAULT 0;

    -- Declaración de variables de sesión para almacenar los resultados
    SET @debt_payment_id = NULL;
    SET @payment_id = NULL;
    SET @amount = NULL;
    SET @ticket_id = NULL;
    SET @ticket_detail_id = NULL;
    SET @customer_id = NULL;

    -- Validar que el tracking_code_input empiece con "PP"
    IF tracking_code_input NOT LIKE 'PP%' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid tracking code: must start with "PP"';
    END IF;

    -- Obtener el ID de parcels_prepaid basado en el tracking_code
    SELECT id, debt INTO prepaid_id, original_debt
    FROM parcels_prepaid
    WHERE tracking_code = tracking_code_input
    LIMIT 1;

    IF prepaid_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tracking code not found';
    END IF;

    -- Asignar valores a las variables de sesión y obtener el id de debt_payment
    SELECT id, payment_id, amount, ticket_id, customer_id
    INTO @debt_payment_id, @payment_id, @amount, @ticket_id, @customer_id
    FROM debt_payment
    WHERE parcel_prepaid_id = prepaid_id
    LIMIT 1;

    -- Obtener el crédito original del cliente
    SELECT credit_available INTO original_credit
    FROM customer
    WHERE id = @customer_id;

    -- Verificar si se obtuvieron resultados
    IF @debt_payment_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No debt_payment record found for this parcel_prepaid_id';
    END IF;

    -- Eliminar los detalles específicos del ticket para el servicio actual
    DELETE FROM tickets_details
    WHERE ticket_id = @ticket_id
      AND detail LIKE CONCAT('%', tracking_code_input, '%');

    -- Verificar si quedan otros detalles en el ticket
    SELECT COUNT(*) INTO remaining_details
    FROM tickets_details
    WHERE ticket_id = @ticket_id;

    -- Si no quedan otros detalles, eliminamos el ticket, payment y cash_out_move
    IF remaining_details = 0 THEN
        -- Verificar si existen registros en cash_out_move para este payment_id
        SELECT COUNT(*) INTO cash_out_move_exists
        FROM cash_out_move
        WHERE payment_id = @payment_id;

        -- Si existen registros en cash_out_move, eliminarlos
        IF cash_out_move_exists > 0 THEN
            DELETE FROM cash_out_move WHERE payment_id = @payment_id LIMIT 1;
        END IF;

        -- Eliminar el registro en payment
        DELETE FROM payment WHERE id = @payment_id LIMIT 1;

        -- Luego eliminar el ticket
        DELETE FROM tickets WHERE id = @ticket_id LIMIT 1;
    END IF;

    -- Finalmente, eliminar el registro en debt_payment usando el ID correcto
    DELETE FROM debt_payment WHERE id = @debt_payment_id LIMIT 1;

    -- Actualizar el valor de debt en parcels_prepaid sumando amount
    UPDATE parcels_prepaid
    SET debt = debt + @amount
    WHERE id = prepaid_id
    LIMIT 1;

    -- Actualizar el credit_available del cliente restando el amount del debt_payment
    UPDATE customer
    SET credit_available = credit_available - @amount
    WHERE id = @customer_id
    LIMIT 1;

    -- Obtener los valores finales después de las operaciones
    SELECT debt INTO final_debt FROM parcels_prepaid WHERE id = prepaid_id;
    SELECT credit_available INTO final_credit FROM customer WHERE id = @customer_id;

    -- Mostrar un resumen de las operaciones realizadas
    SELECT
        prepaid_id AS `Parcels Prepaid ID`,
        @debt_payment_id AS `Debt Payment ID`,
        @payment_id AS `Payment ID`,
        @ticket_id AS `Ticket ID`,
        @customer_id AS `Customer ID`,
        @amount AS `Amount Reverted (Amount)`,
        original_debt AS `Original Debt`,
        final_debt AS `Final Debt`,
        original_credit AS `Original Credit Available`,
        final_credit AS `Final Credit Available`;

END $$

DELIMITER ;

GRANT EXECUTE ON PROCEDURE revert_parcels_prepaid_debt_payment TO 'abordo'@'%';