package com.flab.orderplatform.shared.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.LocalDateTime;
import java.util.List;

@NoRepositoryBean
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {
    @Query("select o.id from OutboxEvent o where o.status = :status and o.createdAt < :threshold")
    List<Long> findIdByStatusAndCreatedAtBefore(OutboxEventStatus status, LocalDateTime threshold, Pageable pageable);

    @Modifying(clearAutomatically = true) // 벌크삭제후 영속성 컨텍스트 동기화
    @Query("delete from OutboxEvent o where o.id in :ids")
    void deleteByIdsInBulk(List<Long> ids);

    List<OutboxEvent> findByStatusIn(List<OutboxEventStatus> outboxEventStatuses, Pageable pageable);
}
