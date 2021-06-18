# Database setup

## Local setup (on developer laptop)

Scripts and configuration to setup the database locally

### Create a local docker image of postgres, and a pgadmin client, accessible via the browser

```
docker-compose -f local-postgres.yml up -d
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

- download Cloud SQL Auth Proxy to you laptop
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

### Drop existing postgres artefacts relating to CC Service.

Note: this will drop all data!

```
./drop_db_gcp.sh
```

### Create postgres artefacts needed for CC Service.

```
./setup_db_gcp.sh
```


