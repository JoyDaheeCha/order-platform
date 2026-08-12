package com.flab.orderplatform.shared.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class OutboxEventRepositoryAdaptor implements OutboxEventRepository {
    private final OutboxEventJpaRepository outboxEventJpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return outboxEventJpaRepository.save(event);
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

    @Override
    public List<OutboxEvent> findEventByStatusIn(List<OutboxEventStatus> statuses, Pageable pageable) {
        return outboxEventJpaRepository.findByStatusIn(statuses, pageable);
    }
}
