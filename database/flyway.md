# Developing with flyway.

We use a popular *evolutionary database* (or *database migration*) tool called [flyway](https://flywaydb.org/)
to help keep our database design in step with our codebase. This was introduced with the SOCINT-19 JIRA,
and chosen after looking at other alternatives.

The purpose of these notes is to guide developers on the practical use of flyway during development.

## Basics of flyway

The migration scripts are normally straightforward SQL files housed in the `src/main/resources/db/migration` folder.

Rules for each SQL migration script:
- Must conform to predefined naming pattern, starting with capital V, followed by the next sequential number, 
  and then 2 underscores followed by a camel-case descriptive name.
- Must be suffixed with ".sql"
- Once applied, each migration script is recorded in a tracking table, and cannot be altered without cleaning the database,
  or at least altering the tracking table (but this may lead to issues, depending on the contents of your migration script).
- the migrations will be run automatically when CCSvc is started.
- if the SQL is incorrect, then the migration will fail on startup, and no change is committed to the DB.
- the configuration is set to validate the SQL against Java entities to ensure they fit together, which will fail to start
  the application if there is a problem (but not after the migration has been applied - so corrective action will be needed
  (see below)

The contents of the database is therefore the combination of all the migration scripts run in order.

## Practicalities of developing new migration scripts

As developers, we will not get everything perfect first time. Development is an interative process and therefore we may work on 
a migration script, e.g. the fictional `V24__AddAuditStructure.sql` , which may add several tables, indexes, and possibly alter
some existing tables. We will typically create JPA Java entities, and other JPA code in lock-step with this SQL. We may take
several iterations at getting our V24 migration script correct. Each time we will need to put the DB into the correct state
to re-apply our next update.

Before our code is merged into the `main` branch, we also usually verify the code in the GCP `DEV` environment. Typically we
may iterate several times with this and have a need to correct the state of the DEV DB.

The PR review process may lead to changes in our V24 migration script. This will be another reason to correct the 
state of the DB, both locally and in DEV.

When you come to use CC `DEV` environment it may have incompatible version state with your current branch. It will need correcting
to match.

In some cases, developers will be developing migration scripts at the same time, so for instance while you are creating your
`V24__AddAuditStructure.sql` , another developer will be creating `V24__RenameUserColumns.sql`.  There are two problems here:
- two version 24's cannot exist at the same time. Whoever loses the race to merge their PR with `main` will need to bump the
  version to `V25`.
- it is likely that the version in `DEV` could interfere with the other developer. Another reason that the `DEV` database needs
  to be altered.

In other words: **there are several likely scenarios where you will need to reset the database in one or more environments, 
so it is able to receive your latest migration script**.

## Rerunning simple migrations

For simple migrations that are *idempotent* in nature and don't change what went before (e.g. they only add new stuff), 
then we can simply delete the migration entry in our `flyway_schema_history` table, and then restart our CCSvc. 
See details in [database readme](README.md) for how to connect using `psql` . 
Using `psql` we could delete our V24 entry like so:
```sql
DELETE FROM cc_schema.flyway_schema_history WHERE version = '24';
``` 
In order to do this successfully our V24 migration script will need to initially delete the items that it is about to create, e.g.
```sql
DROP TABLE IF EXISTS audit_user CASCADE;
```

As you can see, the above has a number of caveats. In many instances it will be easier to clean the database using 
flyway and reapply all migrations. A way to describe this is in the next section.

## Installing and using the `flyway` command-line tool locally.

The `flyway` command-line tool is very useful for inspecting or resetting (cleaning) the database. Here are some example commands:

```sh
flyway                  # print help
flyway info             # print summary of migrations
flyway clean            # clean out all migrations
flyway migrate          # run all migrations (the same process done when starting the CCsvc application)
```

### Installing `flyway`

It can be installed using `homebrew`
```sh
brew install flyway
```

Alternatively, it can also be downloaded and unpacked from the flyway website:
1. Go here: https://flywaydb.org/documentation/usage/commandline/
2. Download and unpack flyway-commandline-7.9.2-macosx-x64.tar.gz   e.g. `cd ~/bin ;  tar xzf ~/Downloads/flyway-commandline-7.9.2-macosx-x64.tar.gz`
3. add to path or pick up from existing path, e.g. cd ~/bin ; `ln -s ~/bin/flyway-79.2/flyway`

Note: the version above is 7.9.2 . This latest version will certainly be different, so adjust instructions as appropriate.

**DO NOT USE `flyway` WITHOUT FIRST CONFIGURING `flyway.conf`**.

### Configuring `flyway` using `flyway.conf`

If you used homebrew to install you will need to create `flyway.conf` in your home directory.
If you unpacked the flyway code manually as described above, you will need to edit `flyway.conf` in the unpacked structure, 
e.g. in `~/bin/flyway-7.9.2/conf/flyway.conf`.

The contents of `flyway.conf` should be something like the following:
```
flyway.url=jdbc:postgresql://localhost:5432/cc
flyway.user=ccadmin
flyway.defaultSchema=cc_schema
flyway.schemas=cc_schema
flyway.locations=filesystem:/Users/cats/project/sdc-int-cc-service/src/main/resources/db/migration
flyway.placeholders.username=ccuser
flyway.password=password
```

Please replace the `flyway.location` value above with your own location pointing to your CC-service code.

Once this is setup then you should be able to verify the command works by issuing `flyway info` which will print a table of the migrations.
e.g:
```
+-----------+---------+--------------------------+------+---------------------+---------+
| Category  | Version | Description              | Type | Installed On        | State   |
+-----------+---------+--------------------------+------+---------------------+---------+
| Versioned | 1       | Creation                 | SQL  | 2021-12-08 09:50:15 | Success |
| Versioned | 2       | CaseTable                | SQL  | 2021-12-08 09:50:16 | Success |
| Versioned | 3       | MessageToSend            | SQL  | 2021-12-08 09:50:16 | Success |
| Versioned | 4       | EventToSend              | SQL  | 2021-12-08 09:50:16 | Success |
| Versioned | 5       | InitialEntityStructure   | SQL  | 2021-12-08 09:50:16 | Success |
| Versioned | 6       | CaseAdditionalFields     | SQL  | 2021-12-08 09:50:16 | Success |
| Versioned | 7       | EventToSendInsertionTime | SQL  | 2021-12-08 09:50:16 | Success |
| Versioned | 8       | CreatedateTimeRenaming   | SQL  | 2021-12-08 09:50:16 | Success |
| Versioned | 9       | UserRoles                | SQL  | 2021-12-08 09:50:16 | Success |
| Versioned | 10      | CohortTypeChange         | SQL  | 2021-12-08 09:50:16 | Success |
| Versioned | 11      | ProductsForSurvey        | SQL  | 2021-12-08 09:50:16 | Success |
+-----------+---------+--------------------------+------+---------------------+---------+
```

Importantly, you will be able to run `flyway clean` if you need to reapply your migrations during iterative development
of your migration scripts.

See https://flywaydb.org/documentation/configuration/configfile.html for more information.

## Using `flyway` against the `DEV` environment

First install `flyway` locally as above.
Also see [database readme](README.md)  on how to connect to `DEV` database with the `cloud_sql_proxy`.

Now do the following:
1. In your first terminal: run `sdc cc dev` to set the kubernetes context to the `DEV` environment
1. run the following:`cloud_sql_proxy -instances=sdc-cc-dev:europe-west2:cc-postgres-dev-6edd80d5=tcp:6432`
1. In another terminal do the following:
    ```
    alias flywaydev='flyway -url=jdbc:postgresql://localhost:6432/cc -user=ccuser -password="$(kubectl get secret db-credentials -o jsonpath='{.data.password}' | base64 --decode)"'
    ```
1. Now you can use `flywaydev` in place of flyway and it will run against `DEV` database.

Note: since your configuration points to your local migration folder, it is possible that `flywaydev info` will report things that are slightly out 
of step with `DEV` , e.g. there may be *Pending* migrations. This is not normally a problem.

In order to clean the `DEV` database, do the following:
1. scale back CCSvc pods to zero: `kubectl scale deployments contactcentresvc --replicas=0`
1. Now clean the database: `flywaydev clean`
1. Now deploy your CCSvc image of choice, eg by pushing a version from `sdc-int-env-version` , or 
1. alternatively, scale back the existing image: `kubectl scale deployments contactcentresvc --replicas=1`

## Considerations

As we get more production ready (or during production updates), database migrations will become more and more important, 
and to guard against problems the following needs consideration:

- The production data will become precious, and migrations must preserve and not corrupt that data. 
  To this end each migration must be thoroughly tested, migrating from realistic production data. 
  **IT IS NOT SUFFICIENT TO JUST TEST A MIGRATION FROM SCRATCH, SINCE THAT WILL MISS REALISTIC PRODUCTION DATA MIGRATION**.
- Migrations will run as the pod is coming up. If you write SQL that takes a long time, this could cause delays in startup
  which could be an issue (although the deployment script has been adjusted to allow for this to some extent). In some
  instances indexing on existing large volumes of data can take quite a lot of time, and we should avoid this, or perhaps
  move the indexing step to a manual update.
- We believe that new deployments to production will happen during a scheduled "downtime", which means our pods can be 
  scheduled back to zero, and the update made. This makes migration scripts much much easier to develop, since we don't 
  have to have staged compatible updates to allow for 24/7 operation (allowing for coexistence of pods at the previous
  version and the new version). This means we can delete / rename table, columns etc in a straightforward way. If this
  ever changes, then this README needs considerable enhancement to guide developers on how to do this.

## A note about style

The existing SQL migration scripts have been written in the following style, and it is helpful to follow this:
- SQL keywords in `UPPER CASE`
- our DB schemas, tables, columes, indexes etc written in `lower_case`
- generated indexes and constraints follow a particular naming convention (e.g. when we use `PRIMARY KEY` or `REFERENCES`,
  then the indexes and constraints are generated for us). If we create our own indexes or constraints we should use the 
  same naming conventions. Also if we rename tables/columns etc, we should also rename the indexes and constraints to be in line.
  An example of these naming conventions is easily seen in `psql` with `\d cc_schema.collection_case`.

## Using the actuator flyway endpoint

When CCSvc is running, you can list migrations in JSON with the following for your local environment:
```sh
curl -s http://localhost:8171/ccsvc/flyway | jq .
```

To do the same for `DEV` environment:
```sh
curl -sk https://cc-dev.int.gcp.onsdigital.uk/ccsvc/flyway | jq .
```

