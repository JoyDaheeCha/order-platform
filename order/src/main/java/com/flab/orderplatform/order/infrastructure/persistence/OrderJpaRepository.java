package com.flab.orderplatform.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@link OrderEntity} 의 JPA 리포지토리
 */
public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
}
