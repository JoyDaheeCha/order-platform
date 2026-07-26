package com.flab.orderplatform.order.infrastructure.message;

import com.flab.orderplatform.order.application.port.out.MessageProducer;
import com.flab.orderplatform.order.application.port.out.OutboxEventRepository;
import com.flab.orderplatform.order.domain.DomainEventThreadManager;
import com.flab.orderplatform.order.domain.OutboxEvent;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 도메인 이벤트 메시지 publisher
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DomainEventMessagePublisher {

    private final MessageProducer messageProducer;
    private final OutboxEventRepository outboxEventRepository;

    /**
     * 트랜잭션 내에서 트랜잭션이 중첩호출될 경우, 동일한 {@link com.flab.orderplatform.order.domain.DomainEvent}가 2번 중복 발행되는것을 방지합니다.
     */
    @Before("execution(* com..*application..*(..)) && @annotation(com.flab.orderplatform.order.application.annotation.OrderTransactional)")
    public void before(JoinPoint joinPoint) {
        var eventsTransactionSynchronization = TransactionSynchronizationManager.getSynchronizations().stream()
                .filter(EventsTransactionSynchronization.class::isInstance)
                .findAny();

        if (eventsTransactionSynchronization.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new EventsTransactionSynchronization(messageProducer, outboxEventRepository));
        }
    }

    @AllArgsConstructor
    private static class EventsTransactionSynchronization implements TransactionSynchronization {

        private final MessageProducer messageProducer;
        private final OutboxEventRepository outboxEventRepository;
        private final List<OutboxEvent> savedOutboxEvents = new ArrayList<>();

        @Override
        public void beforeCommit(boolean readOnly) {
            DomainEventThreadManager.getEvents().forEach(event -> {
                var outboxEvent = outboxEventRepository.save(OutboxEvent.create(event));
                savedOutboxEvents.add(outboxEvent);
            });
        }

        /**
         * 아웃박스의 이벤트를 발행합니다.
         */
        @Override
        public void afterCommit() {
            savedOutboxEvents.forEach(outboxEvent -> {
                try {
                    messageProducer.sendMessage(outboxEvent.getTopic(), outboxEvent.getPayload());
                    outboxEventRepository.save(outboxEvent.complete());
                } catch (Exception e) {
                    log.error("outbox 이벤트 발행 실패 (outboxEventId={}, topic={})", outboxEvent.getId(), outboxEvent.getTopic(), e);
                    outboxEventRepository.save(outboxEvent.fail());
                }
            });
        }

        /**
         * 커밋 성공 여부에 관계없이 트랜잭션이 끝나면 ThreadLocal 을 비웁니다.
         * - 스레드풀 재사용 환경에서 이전 요청의 이벤트가 남아있는것을 방지합니다.
         */
        @Override
        public void afterCompletion(int status) {
            DomainEventThreadManager.clear();
        }
    }
}
