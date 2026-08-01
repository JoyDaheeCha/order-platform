package com.flab.orderplatform.order.application.port.out;

import com.flab.orderplatform.order.domain.OutboxEvent;
import com.flab.orderplatform.order.domain.status.OutboxEventStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> findEventByStatus(OutboxEventStatus status, Pageable pageable);

    List<OutboxEvent> findEventsCreatedAndNeverExecuted(OutboxEventStatus outboxEventStatus, LocalDateTime threshold, Pageable pageable);

    List<Long> findIdByStatusAndCreatedAtBefore(OutboxEventStatus status,
                                                LocalDateTime threshold,
                                                Pageable pageable);

    void deleteByIdsInBulk(List<Long> ids);

    Optional<OutboxEvent> findByEventId(String eventId);
}
