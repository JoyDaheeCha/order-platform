package com.flab.orderplatform.shared.inbox;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class InboxEventRepositoryAdaptor implements InboxEventRepository {

    private final InboxEventJpaRepository inboxEventJpaRepository;

    @Override
    public InboxEvent save(InboxEvent event) {
        return inboxEventJpaRepository.save(event);
    }

    @Override
    public Optional<InboxEvent> findByEventId(String eventId) {
        return inboxEventJpaRepository.findByEventId(eventId);
    }

    @Override
    public List<InboxEvent> findByStatusIn(List<InboxEventStatus> statuses, Pageable pageable) {
        return inboxEventJpaRepository.findByStatusIn(statuses, pageable);
    }
}
