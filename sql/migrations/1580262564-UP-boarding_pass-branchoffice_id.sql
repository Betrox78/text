-- 1580262564 UP boarding_pass-branchoffice_id

ALTER TABLE boarding_pass
  ADD branchoffice_id integer default null;

  UPDATE boarding_pass bp2
   JOIN (select bp.id, co.branchoffice_id from boarding_pass bp
   inner join tickets t on t.id = (
      SELECT t2.id
      FROM tickets AS t2
      WHERE t2.boarding_pass_id = bp.id
      ORDER BY t2.id ASC
      LIMIT 1
   )
   inner join cash_out co on co.id=t.cash_out_id
   where bp.purchase_origin='sucursal') bp3 ON bp2.id = bp3.id
  SET bp2.branchoffice_id = bp3.branchoffice_id;