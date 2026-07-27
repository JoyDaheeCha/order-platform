package com.flab.orderplatform.order.infrastructure.persistence;

import com.flab.orderplatform.order.domain.OutboxEvent;
import com.flab.orderplatform.order.domain.status.OutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatus(OutboxEventStatus outboxEventStatus, Pageable pageable);

    List<OutboxEvent> findByStatusAndCreatedAtBefore(OutboxEventStatus status, LocalDateTime createdAt, Pageable pageable);

    List<Long> findIdByStatusAndCreatedAtBefore(OutboxEventStatus status, LocalDateTime createdAt, Pageable pageable);

    @Modifying(clearAutomatically = true) // 벌크삭제후 영속성 컨텍스트 동기화
    @Query("delete from OutboxEvent o where o.id in:ids")
    void deleteByIdsInBulk(List<Long> ids);
}
