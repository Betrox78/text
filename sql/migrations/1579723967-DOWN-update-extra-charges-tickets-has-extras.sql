-- 1579723967 DOWN update-extra-charges-tickets-has-extras
UPDATE tickets AS t
    INNER JOIN tickets_details AS td ON t.id = td.ticket_id AND t.boarding_pass_id IS NOT NULL AND td.detail = 'Cargos extras en documentación'
SET t.extra_charges = 0.0, t.has_extras = false;