package com.flab.orderplatform.payment.infrastructure.persistence;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
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

import static com.flab.orderplatform.shared.SharedPersistenceConstant.INBOX;
import static com.flab.orderplatform.shared.SharedPersistenceConstant.OUTBOX;

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

    static final String CONTEXT_PACKAGE = "com.flab.orderplatform.payment";

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

    /**
     * Payment 컨텍스트 전용 Flyway 마이그레이션 빈
     */
    @Bean(initMethod = "migrate")
    public Flyway paymentFlyway(@Qualifier("paymentDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .defaultSchema("payment_schema")
                .locations("classpath:db/migration/payment")
                .baselineOnMigrate(true)
                .load();
    }

    @DependsOn("paymentFlyway")
    @Bean
    LocalContainerEntityManagerFactoryBean paymentEntityManagerFactory(
            @Qualifier("paymentDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan(CONTEXT_PACKAGE, OUTBOX, INBOX);
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
