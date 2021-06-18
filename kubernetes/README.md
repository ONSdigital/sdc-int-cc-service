# Notes about deployment descriptor

The purpose of this deployment descriptor is to demonstrate this prototype in DEV environment, running **flyway**, 
connection to **PostgreSQL** provided by **Cloud SQL**.

- This deployment descriptor is tailored for DEV environment **sdc-cc-dev**.
- It is adapted from **sdc-int-cc-terraform/kubernetes/contact-centre-service/contact-centre-service-deployment.yml**
- Configures another container for **Cloud SQL Auth Proxy** , so that the CCSvc can connect on **localhost** on the usual port for PostgreSQL.
- To workaround CPU constraints the CPU resources have been reduced to just 0.5 for bothe the proxy and CCSvc.
- Kubernetes probes have been adjusted to cater for possible long running migrations (see below)
- In order to demonstrate long running migrations, migration 3 reads an environment variable **DELAY_MIGRATION_SECONDS** which has a placeholder
  in the deployment yaml, currently set to 0 seconds. To show this in action: delete the **cc_schema** schema (eg with
  **psql** tool), alter the deployment yaml to set the **DELAY_MIGRATION_SECONDS** to larger value, eg 120 (for 2 minutes), then redeploy
  the CCSvc with this yaml.
- Datasource connection details for both the flyway user and the CCsvc application user are configured with environment variables to 
  override the ones in **application.yml**.

## Kubernetes probes

It is possible that a future flyway migration might take a long time to complete.  
To cater for this the probes have been adjusted as so:

- **startupProbe** has been added to allow for migrations up to 40 minutes long. It uses the Spring supplied actuator readiness endpoint
- Due to the startupProbe, the **initialDelaySeconds** are not needed for readiness and liveness probes.
- **readinessProbe** uses the Spring supplied actuator readiness endpoint
- **livenessProbe** uses the Spring supplied actuator liveness endpoint

## Assumptions

An assumption is made here that a long running migration will take no longer than 40 minutes. If a future migration is thought to
take longer than this (it should be tested before production deployment with similar volumes), then alterations or adaptions will need to be made.

## References

- Connect to Cloud SQL from GKE: https://cloud.google.com/sql/docs/postgres/connect-kubernetes-engine 

