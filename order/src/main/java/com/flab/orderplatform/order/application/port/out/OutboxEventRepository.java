package com.flab.orderplatform.order.application.port.out;

import com.flab.orderplatform.order.domain.OutboxEvent;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);

    OutboxEvent findByAggregateId(String aggregateId);
}
