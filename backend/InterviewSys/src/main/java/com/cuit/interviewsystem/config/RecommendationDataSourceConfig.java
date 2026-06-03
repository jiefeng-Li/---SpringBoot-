package com.cuit.interviewsystem.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class RecommendationDataSourceConfig {

    @Value("${pgsql.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${pgsql.datasource.url}")
    private String url;

    @Value("${pgsql.datasource.username}")
    private String username;

    @Value("${pgsql.datasource.password}")
    private String password;

    @Bean(name = "recommendationDataSource")
    public DataSource recommendationDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean(name = "recommendationJdbcTemplate")
    public JdbcTemplate recommendationJdbcTemplate(@Qualifier("recommendationDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}