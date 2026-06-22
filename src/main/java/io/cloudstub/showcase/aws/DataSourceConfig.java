package io.cloudstub.showcase.aws;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Production datasource whose credentials come from Secrets Manager rather than from configuration.
 * Inactive under the {@code test} profile, where Spring Boot autoconfigures an in-memory database
 * from {@code application-test.yml}.
 */
@Configuration
@Profile("!test")
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.driver-class-name}") String driverClassName,
            DatabaseCredentialsProvider credentials) {
        DbCredentials c = credentials.fetch();
        return DataSourceBuilder.create()
                .url(url)
                .driverClassName(driverClassName)
                .username(c.username())
                .password(c.password())
                .build();
    }
}
