package com.flab.orderplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * 영속화 자동설정 2개를 끈다. (ADR-0004 B-2)
 *
 * <p>둘 다 "DataSource 는 하나"를 전제로 만들어졌다. 컨텍스트별 DataSource 3개(B-2)를 두면
 * 자동설정은 {@code @Primary} 하나만 배선하고 나머지 둘을 방치해 <b>반쯤 동작하는</b> 상태를 만든다.
 * 그 상태는 "왜 이건 되고 저건 안 되나"를 디버깅하기 고약하므로, 아예 끄고 컨텍스트마다
 * DataSource·EntityManagerFactory·TransactionManager 를 전부 손으로 배선한다
 * ({@code {context}.infrastructure.persistence.*PersistenceConfig}).
 *
 * <p><b>끄면 같이 사라지는 것들</b>(수동 배선이 대신 책임진다):
 * <ul>
 *   <li>{@code spring.datasource.*} / {@code spring.jpa.*} 바인딩 → 커스텀 prefix + EMF 직접 주입
 *   <li>Hibernate 기본 속성(네이밍 전략·dialect·ddl-auto) → PersistenceConfig 가 명시
 *   <li>OSIV 인터셉터 → 등록되지 않음(= 기본 미적용, 우리가 원하던 바)
 *   <li>Testcontainers {@code @ServiceConnection} → 꽂아줄 {@code spring.datasource.*} 가 없어 무력.
 *       테스트는 {@code @DynamicPropertySource} 로 직접 매핑한다.
 * </ul>
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
})
public class OrderPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderPlatformApplication.class, args);
    }
}
