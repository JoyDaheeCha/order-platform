package com.flab.orderplatform.order.infrastructure.persistence;

import com.flab.orderplatform.order.domain.OutboxEvent;
import com.flab.orderplatform.order.domain.status.OutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatus(OutboxEventStatus outboxEventStatus, Pageable pageable);

    List<OutboxEvent> findByStatusAndCreatedAtBefore(OutboxEventStatus status, LocalDateTime createdAt, Pageable pageable);
}
