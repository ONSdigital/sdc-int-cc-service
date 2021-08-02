#!/bin/bash
set -e

PGPASSWORD=password psql -v ON_ERROR_STOP=1 -h localhost -U "postgres" -w --dbname "postgres" <<-EOSQL
	CREATE DATABASE cc;
	\c cc
	CREATE USER ccadmin;
	ALTER USER ccadmin WITH PASSWORD 'password';
	CREATE USER ccuser;
	ALTER USER ccuser WITH PASSWORD 'password';
	GRANT ALL PRIVILEGES ON DATABASE cc TO ccadmin;
	CREATE SCHEMA cc_schema AUTHORIZATION ccadmin;
	GRANT USAGE ON SCHEMA cc_schema TO ccuser;
	GRANT SELECT, UPDATE, INSERT, DELETE ON ALL TABLES IN SCHEMA cc_schema TO ccuser;
	GRANT SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA cc_schema TO ccuser;
EOSQL
