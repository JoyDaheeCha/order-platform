package com.flab.orderplatform.payment.infrastructure.persistence;

import com.flab.orderplatform.payment.domain.InboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InboxEventJpaRepository extends JpaRepository<InboxEvent, Long> {
    Optional<InboxEvent> findByEventId(String eventId);
}
