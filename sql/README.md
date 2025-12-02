# Abordo database migrations

## Requirements
Node.js 8.0+  
Yarn 1.10.0+
MySQL 5.7+

## Setup
``` 
> yarn
```

Create an .env file to define the database connection variables within this content (Replace values with valid information):
```
DB_DRIVER=mysql
DB_HOST=localhost
DB_PORT=3306
DB_USER=user
DB_PASSWORD=password
DB_NAME=database 
```

## Create migration files

``` 
> yarn new ${MY_CHANGE}
```

This generate two files, the UP and DOWN for that migration to execute and rollback respectively. Add SQL queries to that files.   
Examples:

``` 
> yarn new add-users
// Output: 
// migrations/1541655193-UP-add-users.sql
// migrations/1541655193-DOWN-add-users.sql
```

## Run migrations

``` 
> yarn migrate
```

## Rollback last migration

``` 
> yarn rollback
```

## Rollback specific migration

``` 
> yarn rollback --target=${VERSION}

```

## Show migrations status

``` 
> yarn status

```