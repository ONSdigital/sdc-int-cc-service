# Database setup

Scripts and configuration to setup the database.

## Create a docker image of postgres, and a pgadmin client, accessible via the browser

```
docker-compose -f local-postgres.yml up -d
```

## Drop existing postgres artefacts relating to CC Service.

Note: this will drop all data!

```
./drop_db.sh
```

## Create postgres artefacts needed for CC Service.

```
./setup_db.sh
```

