package com.flab.orderplatform.order.application.port.out;

import com.flab.orderplatform.order.domain.OutboxEvent;

import java.util.List;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> findFailedEvents();
}
