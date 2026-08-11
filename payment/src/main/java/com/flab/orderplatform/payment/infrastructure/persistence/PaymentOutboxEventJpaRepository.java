package com.flab.orderplatform.payment.infrastructure.persistence;

import com.flab.orderplatform.shared.outbox.OutboxEventJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentOutboxEventJpaRepository extends OutboxEventJpaRepository {
}
