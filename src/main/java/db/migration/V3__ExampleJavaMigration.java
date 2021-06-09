package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V3__ExampleJavaMigration extends BaseJavaMigration {

  private static final String DELAY_SECS = System.getenv("DELAY_MIGRATION_SECONDS");

  @Override
  public void migrate(Context context) throws Exception {

    final JdbcTemplate jdbcTemplate =
        new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));

    // example change
    jdbcTemplate.execute("UPDATE caze SET case_ref = case_ref + 1");

    // simulate a long running migration.
    try {
      int delay = Integer.valueOf(DELAY_SECS);
      if (delay > 0) {
        System.out.println("-- Simulated long running migration starting ---");
      }

      while (delay > 0) {
        System.out.println("Delaying migration: " + delay + " more seconds to wait ...");
        int sleepSecs = Math.min(10, delay);
        Thread.sleep(sleepSecs * 1000);
        delay -= sleepSecs;
      }
    } catch (NumberFormatException e) {
      // carry on, we didn't define a valid delay
    }
  }
}
