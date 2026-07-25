package com.flab.orderplatform.order.infrastructure.message.outbox;

import com.flab.orderplatform.order.application.port.out.MessageProducer;
import com.flab.orderplatform.order.application.port.out.OutboxEventRepository;
import com.flab.orderplatform.order.domain.DomainEvent;
import com.flab.orderplatform.order.domain.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;
import static org.springframework.transaction.event.TransactionPhase.BEFORE_COMMIT;

@Component
@RequiredArgsConstructor
public class OutboxEventHandler {

    private final OutboxEventRepository outboxEventRepository;
    private final MessageProducer messageProducer;

    @TransactionalEventListener(phase = BEFORE_COMMIT)
    public void saveOutboxEvent(DomainEvent domainEvent) {
        outboxEventRepository.save(OutboxEvent.create(domainEvent));
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void publishOutboxEvent(DomainEvent domainEvent) {
        var outboxEvent = outboxEventRepository.findByAggregateId(domainEvent.getAggregateId());
        messageProducer.sendMessage(domainEvent.getTopic(), outboxEvent.getPayload()); // TODO 정상적으로 메시지 발행되는지 확인
        outboxEventRepository.save(outboxEvent.complete());
    }
}
