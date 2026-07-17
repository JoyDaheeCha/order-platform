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
 * Payment 컨텍스트의 영속화 배선. (ADR-0004 결정 B / B-2)
 *
 * <p>order 와 구조가 같다. <b>이 중복은 의도된 비용</b>이다 — ADR-0004 §4 가 "컨텍스트당 영속화
 * {@code @Configuration} 3세트"를 B-2 채택의 대가로 명시하고 감수했다. 공통 헬퍼로 묶으려면
 * 어느 한 컨텍스트나 bootstrap 에 두어야 하는데, 전자는 A-4(컨텍스트 간 의존 금지) 위반이고
 * 후자는 설정이 모듈을 떠나 D3(서비스 분리 용이성)를 해친다. 자율성의 값이다.
 *
 * <p>엔티티는 아직 0개다(첫 테이블은 Day 7). EMF 는 빈 Metamodel 로 정상 부팅하며,
 * 그 "비어 있음"이 곧 Day 2 완료 기준 ② 의 증거다 — 이 EMF 는 order 엔티티를 알지 못한다.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = PaymentPersistenceConfig.CONTEXT_PACKAGE,
        entityManagerFactoryRef = "paymentEntityManagerFactory",
        transactionManagerRef = "paymentTransactionManager"
)
public class PaymentPersistenceConfig {

    static final String CONTEXT_PACKAGE = "com.flab.orderplatform.payment.infrastructure";

    /** Boot 3.4.1 {@code HibernateProperties$Naming} 기본값과 동일. dialect 는 자동 감지에 맡긴다. */
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
