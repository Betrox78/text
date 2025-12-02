-- 1738297104 UP integration-partner-customer-address-seed
DROP TRIGGER IF EXISTS `parcels_AFTER_INSERT_INTEGRATION_PARTNER_CUSTOMER`;

DELIMITER $$

CREATE TRIGGER parcels_AFTER_INSERT_INTEGRATION_PARTNER_CUSTOMER
AFTER INSERT ON parcels
FOR EACH ROW
BEGIN
    DECLARE tg_integration_partner_id INT;
    DECLARE tg_customer_code VARCHAR(64);
    DECLARE tg_integration_partner_customer_id INT;

    IF NEW.integration_partner_session_id IS NOT NULL THEN
        -- Obtener integration_partner_id
        SELECT integration_partner_id 
        INTO tg_integration_partner_id
        FROM integration_partner_session 
        WHERE id = NEW.integration_partner_session_id;

        SET tg_customer_code = MD5(CONCAT(
            UPPER(tg_integration_partner_id), 
            UPPER(NEW.sender_name), 
            UPPER(NEW.sender_last_name), 
            UPPER(NEW.sender_phone), 
            UPPER(NEW.sender_email)
        ));

        -- Verificar si ya existe el customer
        IF NOT EXISTS (
            SELECT 1 FROM integration_partner_customer
            WHERE customer_code = tg_customer_code
        ) THEN
            -- Insertar en integration_partner_customer
            INSERT INTO integration_partner_customer (
                customer_code, 
                integration_partner_id, 
                name, 
                last_name, 
                phone, 
                email
            ) VALUES (
                tg_customer_code,
                tg_integration_partner_id, 
                NEW.sender_name, 
                NEW.sender_last_name,
                NEW.sender_phone, 
                NEW.sender_email
            );

            SELECT id 
            INTO tg_integration_partner_customer_id
            FROM integration_partner_customer
            WHERE customer_code = tg_customer_code;

            -- Insertar en integration_partner_customer_address
            INSERT IGNORE INTO integration_partner_customer_address (
                integration_partner_customer_id, 
                customer_address_id
            ) VALUES (
                tg_integration_partner_customer_id, 
                NEW.sender_address_id
            );
        END IF;
    END IF;
END$$

DELIMITER ;
