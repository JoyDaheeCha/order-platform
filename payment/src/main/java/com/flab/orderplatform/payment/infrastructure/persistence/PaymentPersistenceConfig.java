package com.flab.orderplatform.payment.infrastructure.persistence;

import java.util.Map;

import javax.sql.DataSource;

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

import jakarta.persistence.EntityManagerFactory;

/**
 * Payment 컨텍스트의 영속화 설정
 */
@Configuration
@EnableJpaRepositories(
        basePackages = PaymentPersistenceConfig.CONTEXT_PACKAGE,
        entityManagerFactoryRef = "paymentEntityManagerFactory",
        transactionManagerRef = "paymentTransactionManager"
)
public class PaymentPersistenceConfig {

    static final String CONTEXT_PACKAGE = "com.flab.orderplatform.payment.infrastructure";

    private static final Map<String, String> HIBERNATE_PROPERTIES = Map.of(
            "hibernate.hbm2ddl.auto", "validate",
            "hibernate.physical_naming_strategy", CamelCaseToUnderscoresNamingStrategy.class.getName(),
            "hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName()
    );

    @Bean
    @ConfigurationProperties("datasource.payment")
    DataSourceProperties paymentDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    DataSource paymentDataSource() {
        return paymentDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    LocalContainerEntityManagerFactoryBean paymentEntityManagerFactory(
            @Qualifier("paymentDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan(CONTEXT_PACKAGE);
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setJpaPropertyMap(HIBERNATE_PROPERTIES);
        emf.setPersistenceUnitName("payment");
        return emf;
    }

    @Bean
    PlatformTransactionManager paymentTransactionManager(
            @Qualifier("paymentEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
