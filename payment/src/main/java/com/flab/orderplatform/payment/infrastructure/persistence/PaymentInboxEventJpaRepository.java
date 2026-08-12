package com.flab.orderplatform.payment.infrastructure.persistence;

import com.flab.orderplatform.shared.inbox.InboxEventJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentInboxEventJpaRepository extends InboxEventJpaRepository {
}
