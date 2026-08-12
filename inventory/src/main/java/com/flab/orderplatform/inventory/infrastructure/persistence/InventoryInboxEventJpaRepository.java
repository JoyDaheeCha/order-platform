package com.flab.orderplatform.inventory.infrastructure.persistence;

import com.flab.orderplatform.shared.inbox.InboxEventJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryInboxEventJpaRepository extends InboxEventJpaRepository {
}
