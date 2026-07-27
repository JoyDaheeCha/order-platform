package com.flab.orderplatform.order.infrastructure.persistence;

import com.flab.orderplatform.order.application.port.out.OutboxEventRepository;
import com.flab.orderplatform.order.domain.OutboxEvent;
import com.flab.orderplatform.order.domain.status.OutboxEventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryAdaptor implements OutboxEventRepository {
    private final OutboxEventJpaRepository outboxEventJpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return outboxEventJpaRepository.save(event);
    }

    @Override
    public List<OutboxEvent> findEventByStatus(OutboxEventStatus status, Pageable pageable) {
        return outboxEventJpaRepository.findByStatus(status, pageable);
    }

    @Override
    public List<OutboxEvent> findEventsCreatedAndNeverExecuted(OutboxEventStatus status,
                                                               LocalDateTime threshold,
                                                               Pageable pageable) {
        return outboxEventJpaRepository.findByStatusAndCreatedAtBefore(status, threshold, pageable);
    }

    @Override
    public List<Long> findIdByStatusAndCreatedAtBefore(OutboxEventStatus status,
                                                       LocalDateTime threshold,
                                                       Pageable pageable) {
        return outboxEventJpaRepository.findIdByStatusAndCreatedAtBefore(status, threshold, pageable);
    }

    @Override
    public void deleteByIdsInBulk(List<Long> ids) {
        outboxEventJpaRepository.deleteByIdsInBulk(ids);
    }
}
