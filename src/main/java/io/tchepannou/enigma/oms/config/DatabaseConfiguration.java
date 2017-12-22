package io.tchepannou.enigma.oms.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
public class DatabaseConfiguration extends HikariConfig{
    @Bean(destroyMethod = "close")
    DataSource dataSource() {
        return new HikariDataSource(this);
    }
}
