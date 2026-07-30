package com.flab.orderplatform.payment.application.port.out;

import com.flab.orderplatform.payment.domain.InboxEvent;

public interface InboxEventRepository {
    InboxEvent save(InboxEvent event);
}
