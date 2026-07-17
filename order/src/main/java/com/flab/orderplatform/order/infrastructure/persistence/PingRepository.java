package com.flab.orderplatform.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <b>배관 점검용 더미</b> — {@link PingEntity} 와 함께 Day 4 에서 삭제한다(todo.md).
 *
 * <p>EntityManager 를 직접 쓰지 않고 리포지토리를 두는 이유: {@code @EnableJpaRepositories} 의
 * {@code entityManagerFactoryRef}·{@code transactionManagerRef} 배선까지 함께 증명하기 위해서다.
 * 이게 틀리면 "save 는 성공했는데 DB 엔 없는" 조용한 실패가 난다(OrderPersistenceConfig 참고).
 */
public interface PingRepository extends JpaRepository<PingEntity, String> {
}
