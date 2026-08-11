package com.flab.orderplatform.shared.inbox;


import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface InboxEventRepository {
    InboxEvent save(InboxEvent event);

    Optional<InboxEvent> findByEventId(String eventId);

    List<InboxEvent> findByStatusIn(List<InboxEventStatus> statuses, Pageable pageable);
}
