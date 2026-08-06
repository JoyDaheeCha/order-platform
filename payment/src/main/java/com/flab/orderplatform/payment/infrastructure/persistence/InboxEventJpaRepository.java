package com.flab.orderplatform.payment.infrastructure.persistence;

import com.flab.orderplatform.payment.domain.InboxEvent;
import com.flab.orderplatform.payment.domain.status.InboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InboxEventJpaRepository extends JpaRepository<InboxEvent, Long> {
    Optional<InboxEvent> findByEventId(String eventId);

    List<InboxEvent> findByStatusIn(List<InboxEventStatus> statuses, Pageable pageable);
}
