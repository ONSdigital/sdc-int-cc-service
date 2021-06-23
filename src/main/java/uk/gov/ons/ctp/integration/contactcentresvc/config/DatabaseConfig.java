package uk.gov.ons.ctp.integration.contactcentresvc.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseConfig {

  /**
   * See Spring Boot guide. By redefining the datasource properties, then this overrides the normal
   * spring properties, to keep the "ccdb" prefix consistent, and allows the usual "url" style
   * configuration.
   *
   * <p>This also requires the datasource to be a specific type (Hikari by default).
   *
   * @return data source properties
   */
  @Bean
  @Primary
  @ConfigurationProperties("ccdb.datasource")
  public DataSourceProperties dataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean(destroyMethod = "close")
  @Primary
  @ConfigurationProperties("ccdb.datasource")
  public HikariDataSource ccDataSource(DataSourceProperties properties) {
    return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }

  @Bean
  @Primary
  @ConfigurationProperties("ccdb.datasource")
  public JdbcTemplate ccJdbcTemplate(DataSource datasource) {
    return new JdbcTemplate(datasource);
  }

  // ---- flyway ....

  @Bean
  @ConfigurationProperties("ccdb-admin.datasource")
  public DataSourceProperties adminDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean(destroyMethod = "close")
  @FlywayDataSource
  @ConfigurationProperties("ccdb-admin.datasource")
  public HikariDataSource adminDataSource(DataSourceProperties properties) {
    return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }
}
