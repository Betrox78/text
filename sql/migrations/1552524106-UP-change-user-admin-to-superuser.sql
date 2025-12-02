-- 1552524106 UP change user admin to superuser
update users set user_type = 'S' where id = 1;