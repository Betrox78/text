-- 1552524106 DOWN change user admin to superuser
update users set user_type = 'A' where id = 1;