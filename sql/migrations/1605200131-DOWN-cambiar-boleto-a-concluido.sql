-- 1605200131 DOWN cambiar-boleto-a-concluido
update boarding_pass set boardingpass_status = 0 where reservation_code = 'B2010C1C7BC';
update boarding_pass set boardingpass_status = 0 where reservation_code = 'B201079D5DF';