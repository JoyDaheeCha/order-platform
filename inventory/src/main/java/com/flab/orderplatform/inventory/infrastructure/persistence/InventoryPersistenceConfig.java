package com.flab.orderplatform.inventory.infrastructure.persistence;

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
 * Inventory 컨텍스트의 영속화 설정
 */
@Configuration
@EnableJpaRepositories(
        basePackages = InventoryPersistenceConfig.CONTEXT_PACKAGE,
        entityManagerFactoryRef = "inventoryEntityManagerFactory",
        transactionManagerRef = "inventoryTransactionManager"
)
public class InventoryPersistenceConfig {

    static final String CONTEXT_PACKAGE = "com.flab.orderplatform.inventory";

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
