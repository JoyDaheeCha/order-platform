package com.flab.orderplatform.order.infrastructure.persistence;

import com.flab.orderplatform.order.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {
}
