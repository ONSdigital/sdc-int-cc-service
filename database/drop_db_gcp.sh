#!/bin/bash
# 
# Drop GCP database artefacts
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

echo "All CCsvc database data will be removed in CloudSQL connected via the Cloud SQL Auth Proxy."
echo -n "Continue (y/n)? "
read answer
if [ "$answer" != "${answer#[Yy]}" ] ;then
  echo "Dropping data ..."
else
  echo "Nothing changed"
  exit 0
fi

USER=ccuser
PORT=6432

PGPASSWORD=$GCP_PGPPWD psql -v ON_ERROR_STOP=1 -h localhost -p $PORT -U "$USER" -w --dbname "cc" <<-EOSQL
	DROP SCHEMA IF EXISTS cc_schema CASCADE;
EOSQL
