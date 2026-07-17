package com.flab.orderplatform.order.infrastructure.persistence;

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
 * Order 컨텍스트의 영속화 배선. (ADR-0004 결정 B / B-2)
 *
 * <p><b>왜 손으로 배선하나</b>: 자동설정은 "DataSource 는 하나"를 전제한다. 컨텍스트마다
 * DataSource 를 두면(B-2) 자동설정이 {@code @Primary} 하나만 배선하고 나머지를 방치하므로
 * bootstrap 에서 아예 끄고(OrderPlatformApplication) 3세트를 각자의 컨텍스트가 소유한다.
 * 설정이 모듈과 함께 움직여야 후일 서비스 분리가 평이하다(ADR-0004 D3).
 *
 * <p><b>이 클래스가 경계를 만든다</b>: {@code packagesToScan} 이 order 패키지뿐이므로 이 EMF 의
 * Metamodel 에는 order 엔티티만 들어간다. payment/inventory 의 EMF 는 order 엔티티를
 * <b>클래스로서 알지 못한다</b> — cross-schema 접근이 "금지"가 아니라 <b>불가능</b>해지는 지점이다(C-2).
 *
 * <p><b>{@code @Primary} 를 일부러 안 붙인다</b>: 붙이면 한정자 없는 {@code @Transactional} 이
 * 조용히 그 하나를 쓴다. order 리포지토리가 payment 의 TransactionManager 아래서 도는데도
 * 예외 없이 "save 는 성공했는데 DB 엔 없는" 상태가 된다. 후보가 셋이면 Spring 이 시끄럽게 실패한다.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = OrderPersistenceConfig.CONTEXT_PACKAGE,
        entityManagerFactoryRef = "orderEntityManagerFactory",
        transactionManagerRef = "orderTransactionManager"
)
public class OrderPersistenceConfig {

    static final String CONTEXT_PACKAGE = "com.flab.orderplatform.order.infrastructure";

    /**
     * 수동 EMF 는 {@code spring.jpa.*} 를 받지 못한다 — Boot 가 넣어주던 것을 여기서 직접 넣는다.
     * 값은 Boot 3.4.1 의 {@code HibernateProperties$Naming} 기본값과 동일하게 맞췄다.
     *
     * <p>dialect 는 일부러 안 넣는다: Boot 도 {@code spring.jpa.database-platform} 이 없으면
     * 설정하지 않고 Hibernate 의 JDBC 메타데이터 자동 감지에 맡긴다. 서버 실제 버전을 읽어
     * 버전별 SQL 을 쓰므로 그편이 낫다.
     */
    private static final Map<String, String> HIBERNATE_PROPERTIES = Map.of(
            // 스키마의 주인은 앱이 아니다 — 엔티티와 실제 테이블이 어긋나면 부팅을 깬다.
            "hibernate.hbm2ddl.auto", "validate",
            // ⚠️ Boot 3 에서 SpringPhysicalNamingStrategy 는 사라졌다. Hibernate 자체 클래스로 대체됐다.
            //    빠뜨리면 `noteText` 가 snake_case 로 안 바뀌어 validate 가 부팅을 깬다.
            "hibernate.physical_naming_strategy", CamelCaseToUnderscoresNamingStrategy.class.getName(),
            "hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class.getName()
    );

    /** {@code spring.datasource.*} 가 아니라 우리 prefix — 자동설정을 껐으므로 그 키는 아무도 안 읽는다. */
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
        // ★ 경계의 실체: 이 EMF 는 order 패키지의 엔티티만 스캔한다.
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
