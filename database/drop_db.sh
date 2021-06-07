#!/bin/bash
set -e

PGPASSWORD=password psql -v ON_ERROR_STOP=1 -h localhost -U "postgres" -w --dbname "postgres" <<-EOSQL
	\c ccdb
	REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA cc_schema FROM ccsvc;
	REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA cc_schema FROM ccsvc;
	DROP OWNED BY ccsvc;
	DROP USER IF EXISTS ccsvc;
	DROP OWNED BY ccadmin;
	DROP USER IF EXISTS ccadmin;
	-- DROP SCHEMA IF EXISTS cc_schema;
	\c postgres
	SELECT pg_terminate_backend(pid) FROM  pg_stat_activity WHERE datname='ccdb';
	DROP DATABASE IF EXISTS ccdb;
EOSQL
