package com.flab.orderplatform.payment.application.port.out;

import com.flab.orderplatform.payment.domain.InboxEvent;

import java.util.Optional;

public interface InboxEventRepository {
    InboxEvent save(InboxEvent event);

    Optional<Object> findByEventId(String eventId);
}
