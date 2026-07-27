package com.flab.orderplatform.order.infrastructure.persistence;

import com.flab.orderplatform.order.domain.OutboxEvent;
import com.flab.orderplatform.order.domain.status.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findOutboxEventByStatus(OutboxEventStatus outboxEventStatus);
}
