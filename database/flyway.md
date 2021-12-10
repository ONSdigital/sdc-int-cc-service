# Developing with flyway.

We use a popular *evolutionary database* or *database migration* tool called [flyway](https://flywaydb.org/)
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

Finally, the PR review process may lead to changes in our V24 migration script. This will be another reason to correct the 
state of the DB, both locally and in DEV.

## WRITEME more


