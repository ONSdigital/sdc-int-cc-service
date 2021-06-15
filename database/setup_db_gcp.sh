#!/bin/bash
# 
# Setup GCP database
#
# Before running this: 
#	export GCP_PGPPWD="<password>"
#	set running Cloud SQL Auth Proxy on port $PORT.
#
# assumes :
# 	ccuser user already created
#	cc database already created
#
set -e

USER=ccuser
PORT=6432

PGPASSWORD=$GCP_PGPPWD psql -v ON_ERROR_STOP=1 -h localhost -p $PORT -U "$USER" -w --dbname "cc" <<-EOSQL
	CREATE SCHEMA cc_schema AUTHORIZATION ccuser;
EOSQL
