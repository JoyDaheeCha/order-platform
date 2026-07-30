package com.flab.orderplatform.payment.infrastructure.persistence;

import com.flab.orderplatform.payment.application.port.out.InboxEventRepository;
import com.flab.orderplatform.payment.domain.InboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
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
}
