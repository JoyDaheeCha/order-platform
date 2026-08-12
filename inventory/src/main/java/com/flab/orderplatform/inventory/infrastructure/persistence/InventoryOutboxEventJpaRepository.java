package com.flab.orderplatform.inventory.infrastructure.persistence;

import com.flab.orderplatform.shared.outbox.OutboxEventJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryOutboxEventJpaRepository extends OutboxEventJpaRepository {
}
