package com.flab.orderplatform.shared.inbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface InboxEventJpaRepository extends JpaRepository<InboxEvent, Long> {
    Optional<InboxEvent> findByEventId(String eventId);

    List<InboxEvent> findByStatusIn(List<InboxEventStatus> statuses, Pageable pageable);
}
