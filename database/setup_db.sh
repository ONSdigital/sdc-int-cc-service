#!/bin/bash
set -e

PGPASSWORD=password psql -v ON_ERROR_STOP=1 -h localhost -U "postgres" -w --dbname "postgres" <<-EOSQL
	CREATE DATABASE ccdb;
	\c ccdb
	CREATE USER ccadmin;
	ALTER USER ccadmin WITH PASSWORD 'password';
	CREATE USER ccsvc;
	ALTER USER ccsvc WITH PASSWORD 'password';
	GRANT ALL PRIVILEGES ON DATABASE ccdb TO ccadmin;
	CREATE SCHEMA cc_schema AUTHORIZATION ccadmin;
	GRANT USAGE ON SCHEMA cc_schema TO ccsvc;
	GRANT SELECT, UPDATE, INSERT, DELETE ON ALL TABLES IN SCHEMA cc_schema TO ccsvc;
	GRANT SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA cc_schema TO ccsvc;
EOSQL
