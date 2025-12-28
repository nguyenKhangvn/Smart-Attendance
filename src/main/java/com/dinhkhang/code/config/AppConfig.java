package com.dinhkhang.code.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = "com.dinhkhang.code", excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Controller.class))
@EnableJpaRepositories(basePackages = "com.dinhkhang.code.repository")
@EnableTransactionManagement
@EnableScheduling  // Enable scheduled tasks
@PropertySource("classpath:application.properties")
public class AppConfig {

    @Autowired
    private Environment env;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(env.getProperty("db.url", "jdbc:mysql://localhost:3306/smart_attendance"));
        config.setUsername(env.getProperty("db.username", "root"));
        config.setPassword(env.getProperty("db.password", "123"));
        config.setDriverClassName(env.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));

        // HikariCP settings
        config.setMaximumPoolSize(Integer.parseInt(env.getProperty("db.pool.size", "10")));
        config.setMinimumIdle(Integer.parseInt(env.getProperty("db.pool.min-idle", "5")));
        config.setConnectionTimeout(Long.parseLong(env.getProperty("db.pool.connection-timeout", "30000")));
        config.setIdleTimeout(Long.parseLong(env.getProperty("db.pool.idle-timeout", "600000")));
        config.setMaxLifetime(Long.parseLong(env.getProperty("db.pool.max-lifetime", "1800000")));

        return new HikariDataSource(config);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan("com.dinhkhang.code.entity");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(hibernateProperties());

        return em;
    }

    private Properties hibernateProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.dialect", env.getProperty("hibernate.dialect",
                "org.hibernate.dialect.MySQL8Dialect"));
        properties.put("hibernate.show_sql", env.getProperty("hibernate.show_sql", "true"));
        properties.put("hibernate.format_sql", env.getProperty("hibernate.format_sql", "true"));
        properties.put("hibernate.hbm2ddl.auto", env.getProperty("hibernate.hbm2ddl.auto", "update"));
        properties.put("hibernate.jdbc.batch_size", env.getProperty("hibernate.jdbc.batch_size", "20"));
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        return properties;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }
}
