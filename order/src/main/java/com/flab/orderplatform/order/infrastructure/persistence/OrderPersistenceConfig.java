package com.flab.orderplatform.order.infrastructure.persistence;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    private static final Map<String, String> HIBERNATE_PROPERTIES = Map.of(
            "hibernate.hbm2ddl.auto", "validate",
            "hibernate.physical_naming_strategy", CamelCaseToUnderscoresNamingStrategy.class.getName(),
            "hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName()
    );

    @Bean
    @ConfigurationProperties("datasource.order")
    DataSourceProperties orderDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    DataSource orderDataSource() {
        return orderDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    LocalContainerEntityManagerFactoryBean orderEntityManagerFactory(
            @Qualifier("orderDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan(CONTEXT_PACKAGE);
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setJpaPropertyMap(HIBERNATE_PROPERTIES);
        emf.setPersistenceUnitName("order");
        return emf;
    }

    @Bean
    PlatformTransactionManager orderTransactionManager(
            @Qualifier("orderEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
