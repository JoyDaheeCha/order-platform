package com.flab.orderplatform.order.infrastructure.persistence;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Order 컨텍스트의 영속화 설정
 */
@Configuration
@EnableJpaRepositories(
        basePackages = OrderPersistenceConfig.CONTEXT_PACKAGE,
        entityManagerFactoryRef = "orderEntityManagerFactory",
        transactionManagerRef = "orderTransactionManager"
)
public class OrderPersistenceConfig {

    static final String CONTEXT_PACKAGE = "com.flab.orderplatform.order";

    @Bean
    @ConfigurationProperties("datasource.order")
    DataSourceProperties orderDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    DataSource orderDataSource() {
        return orderDataSourceProperties().initializeDataSourceBuilder().build();
    }

    /**
     * Order 컨텍스트 전용 Flyway 마이그레이션 빈
     */
    @Bean(initMethod = "migrate")
    public Flyway orderFlyway(@Qualifier("orderDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .defaultSchema("order_schema")
                .locations("classpath:db/migration/order")
                .baselineOnMigrate(true)
                .load();
    }

    @DependsOn("orderFlyway")
    @Bean
    LocalContainerEntityManagerFactoryBean orderEntityManagerFactory(
            @Qualifier("orderDataSource") DataSource dataSource,
            @Value("${order.jpa.hibernate.ddl-auto:validate}") String ddlAuto) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan(CONTEXT_PACKAGE);
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setJpaPropertyMap(Map.of(
                "hibernate.hbm2ddl.auto", ddlAuto,
                "hibernate.physical_naming_strategy", CamelCaseToUnderscoresNamingStrategy.class.getName(),
                "hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName(),
                "hibernate.format_sql", true
        ));
        emf.setPersistenceUnitName("order");
        return emf;
    }

    @Bean
    PlatformTransactionManager orderTransactionManager(
            @Qualifier("orderEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
