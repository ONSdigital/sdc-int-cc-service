# Database setup

## Local setup (on developer laptop)

Scripts and configuration to setup the database locally.
The setup script MUST be run before you startup the CCSvc.

### Create a local docker image of postgres, and a pgadmin client, accessible via the browser

```
docker-compose -f local-postgres.yml up -d
```

### Using pgadmin local client in the browser.

Navigate to http://localhost:1963 .

Enter credentials:
- username: ons@ons.gov
- password: secret

You will need to create a server context, for example, to connect locally:

1. Object -> Create -> Server ...
1. Give it a name, e.g. "ccsvc"
1. Click on the connection tab:
    1. `postgres` for the host name (This is important!)
    1. `5432` for the port
    1. `postgres` for the database name
    1. `postgres` for the username
    1. `password` for the password
1. Click save to close the dialog and connect to the postgreSQL instance
1. Expand the Server section on the left to see the various DB elements

After you run the **setup_db.sh** (see below), then you should see the **cc** Database and associated **cc_schema** by navigating in the sidebar.

### Prerequisites to using the drop or setup scripts

Install **psql** (command line PostgreSQL client) locally.

```
brew install libpq
brew link --force libpq
```

Here are a few handy PSQL commands:

```
\?                  -- command help
\l                  -- list databases
\c cc               -- change to cc database
\d                  -- describe (anything)
\d cc_schema.caze   -- describe table "caze"
\di                 -- list indexes
\dn                 -- list schemas
\conninfo           -- connection information: DB, user, host, port
\dt cc_schema.*     -- list tables in cc_schema schema
\du                 -- list roles (users)

select * from cc_schema.flyway_schema_history;
\d cc_schema.caze
```

### Drop existing postgres artefacts relating to CC Service.

Note: this will drop all data!

```
./drop_db.sh
```

### Create postgres artefacts needed for CC Service.

```
./setup_db.sh
```

## Setup against GCP deployed CloudSQL database (e.g. DEV environment)

### Prerequisites:

- download Cloud SQL Auth Proxy to your laptop
- make sure GOOGLE_APPLICATION_CREDENTIALS is not set since it will interfere
- change context to the GCP environment
- set running Cloud SQL Auth Proxy on port 6432
- export GCP_PGPPWD with the dababase password for the CC user

Example:

```
    curl -o cloud_sql_proxy https://dl.google.com/cloudsql/cloud_sql_proxy.darwin.amd6
    chmod +x cloud_sql_proxy

    unset GOOGLE_APPLICATION_CREDENTIALS
    sdc cc dev
    ./cloud_sql_proxy -instances=sdc-cc-dev:europe-west2:cc-postgres-dev-6edd80d5=tcp:6432
```

In another terminal ...

```
    export GCP_PGPPWD=$(kubectl get secret db-credentials -o jsonpath='{.data.password}' | base64 -d)

    # run scripts below in this terminal
```

### Drop existing postgres artefacts relating to CC Service in GCP

Note: this will drop all data!

```
./drop_db_gcp.sh
```

### Create postgres artefacts needed for CC Service.

Users and DB are pre-created by terraform, and flyway by default will create schema if it is missing, so no script needed!


