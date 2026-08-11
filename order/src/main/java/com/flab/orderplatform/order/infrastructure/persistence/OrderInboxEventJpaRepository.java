package com.flab.orderplatform.order.infrastructure.persistence;

import com.flab.orderplatform.shared.inbox.InboxEventJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderInboxEventJpaRepository extends InboxEventJpaRepository {
}
