package com.flab.orderplatform.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@link OrderEntity} 의 JPA 리포지토리.
 *
 * <p><b>이름이 {@code OrderRepository} 가 아닌 이유</b>: Day 4 의 아웃바운드 포트가 그 이름을
 * 가져간다(application 이 소유하는 인터페이스, DIP). 이건 infra 쪽 구현 디테일이므로 {@code Jpa} 를 붙여
 * 자리를 비워 둔다.
 *
 * <p>EntityManager 를 직접 쓰지 않고 리포지토리를 두는 이유: {@code @EnableJpaRepositories} 의
 * {@code entityManagerFactoryRef}·{@code transactionManagerRef} 배선까지 함께 증명하기 위해서다.
 * 이게 틀리면 "save 는 성공했는데 DB 엔 없는" 조용한 실패가 난다(OrderPersistenceConfig 참고).
 */
public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {
}
