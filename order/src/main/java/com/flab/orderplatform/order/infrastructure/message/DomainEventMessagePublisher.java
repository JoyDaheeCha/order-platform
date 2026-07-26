package com.flab.orderplatform.order.infrastructure.message;

import com.flab.orderplatform.order.application.port.out.MessageProducer;
import com.flab.orderplatform.order.domain.DomainEventThreadManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;

// TODO afterCommit은 트랜잭션 이후에 실행되도록 PROPAGATION_REQUIRES_NEW 를 사용해야하는데, 우리는 도메인 이벤트를 발행해서 outbox에 매번 저장해야하니까 이게 맞는지 확인 필요

/**
 * 도메인 이벤트 메시지 publisher
 */
@Component
@RequiredArgsConstructor
public class DomainEventMessagePublisher implements TransactionSynchronization {

    private final MessageProducer messageProducer;

    /**
     * 스레드에 쌓아둔 이벤트를 카프카로 발행합니다.
     */
    @Override
    public void afterCommit() {
        DomainEventThreadManager.getEvents()
                .forEach(
                        event -> {
                            messageProducer.sendMessage(event.getTopic(), event);
                        }
                );
    }

    @Override
    public void afterCompletion(int status) {
        DomainEventThreadManager.clear();
    }
}
