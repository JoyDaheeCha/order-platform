package com.flab.orderplatform.order.infrastructure.persistence;

import com.flab.orderplatform.shared.outbox.OutboxEventJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderOutboxEventJpaRepository extends OutboxEventJpaRepository {
}
