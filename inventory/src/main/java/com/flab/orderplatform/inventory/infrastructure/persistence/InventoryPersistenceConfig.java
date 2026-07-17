package com.flab.orderplatform.inventory.infrastructure.persistence;

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
 * Inventory 컨텍스트의 영속화 배선. (ADR-0004 결정 B / B-2)
 *
 * <p>구조·중복의 근거는 {@code OrderPersistenceConfig}·{@code PaymentPersistenceConfig} 와 같다
 * (ADR-0004 §4 가 명시한 "컨텍스트당 영속화 3세트"의 비용).
 *
 * <p>엔티티는 아직 0개다 — 첫 테이블 {@code stocks} 는 Day 8 에 온다.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = InventoryPersistenceConfig.CONTEXT_PACKAGE,
        entityManagerFactoryRef = "inventoryEntityManagerFactory",
        transactionManagerRef = "inventoryTransactionManager"
)
public class InventoryPersistenceConfig {

    static final String CONTEXT_PACKAGE = "com.flab.orderplatform.inventory.infrastructure";

    /** Boot 3.4.1 {@code HibernateProperties$Naming} 기본값과 동일. dialect 는 자동 감지에 맡긴다. */
    private static final Map<String, String> HIBERNATE_PROPERTIES = Map.of(
            "hibernate.hbm2ddl.auto", "validate",
            "hibernate.physical_naming_strategy", CamelCaseToUnderscoresNamingStrategy.class.getName(),
            "hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName()
    );

    @Bean
    @ConfigurationProperties("datasource.inventory")
    DataSourceProperties inventoryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    DataSource inventoryDataSource() {
        return inventoryDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    LocalContainerEntityManagerFactoryBean inventoryEntityManagerFactory(
            @Qualifier("inventoryDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan(CONTEXT_PACKAGE);
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setJpaPropertyMap(HIBERNATE_PROPERTIES);
        emf.setPersistenceUnitName("inventory");
        return emf;
    }

    @Bean
    PlatformTransactionManager inventoryTransactionManager(
            @Qualifier("inventoryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
