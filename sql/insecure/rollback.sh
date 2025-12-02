#!/bin/sh
for i in $( ls migrations/*DOWN*.sql ); do
  mysql -u $DB_USER -p$DB_PASSWORD -D $DB_NAME -h $DB_HOST -P $DB_PORT < $i
done