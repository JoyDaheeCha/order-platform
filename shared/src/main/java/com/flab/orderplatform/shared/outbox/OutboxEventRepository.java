package com.flab.orderplatform.shared.outbox;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);

    List<Long> findIdByStatusAndCreatedAtBefore(OutboxEventStatus status,
                                                LocalDateTime threshold,
                                                Pageable pageable);

    void deleteByIdsInBulk(List<Long> ids);

    List<OutboxEvent> findEventByStatusIn(List<OutboxEventStatus> statuses, Pageable pageable);
}
