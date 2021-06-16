#!/bin/bash
set -e

PGPASSWORD=password psql -v ON_ERROR_STOP=1 -h localhost -U "postgres" -w --dbname "postgres" <<-EOSQL
	\c cc
	REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA cc_schema FROM ccuser;
	REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA cc_schema FROM ccuser;
	DROP OWNED BY ccuser;
	DROP USER IF EXISTS ccuser;
	DROP OWNED BY ccadmin;
	DROP USER IF EXISTS ccadmin;
	-- DROP SCHEMA IF EXISTS cc_schema;
	\c postgres
	SELECT pg_terminate_backend(pid) FROM  pg_stat_activity WHERE datname='cc';
	DROP DATABASE IF EXISTS cc;
EOSQL
