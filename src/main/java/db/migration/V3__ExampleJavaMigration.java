package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class V3__ExampleJavaMigration extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {

    final JdbcTemplate jdbcTemplate =
        new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));

    // example change
    jdbcTemplate.execute("UPDATE caze SET case_ref = case_ref + 1");
  }
}
