package com.flab.orderplatform.payment.infrastructure.message.inbox;

import com.flab.orderplatform.payment.application.InboxEventCommandHandler;
import com.flab.orderplatform.payment.application.annotation.Inbox;
import com.flab.orderplatform.payment.application.command.InboxEventCreateCommand;
import com.flab.orderplatform.payment.application.port.out.InboxEventRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 인박스 패턴 적용 aspect
 * {@link Inbox} 보다 나중에 수행
 */
@Order
@Aspect
@Component
@RequiredArgsConstructor
public class InboxAspect {
    private final InboxEventCommandHandler inboxEventCommandHandler;
    private final InboxEventRepository inboxEventRepository;

    private static @NonNull ConsumerRecord<String, String> getConsumerRecord(ProceedingJoinPoint joinPoint) {
        return Arrays.stream(joinPoint.getArgs())
                .filter(ConsumerRecord.class::isInstance)
                .map(arg -> (ConsumerRecord<String, String>) arg)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("@Inbox 애노테이션은 ConsumerRecord 파라메터가 필요합니다."));
    }

    private static String getHeaderValueByKey(ConsumerRecord<String, String> consumerRecord, String headerKey) {
        var header = consumerRecord.headers().lastHeader(headerKey);
        return new String(header.value());
    }

    @Around("@annotation(inbox)")
    public Object handle(ProceedingJoinPoint joinPoint, Inbox inbox) throws Throwable {

        var consumerRecord = getConsumerRecord(joinPoint);
        var eventId = getHeaderValueByKey(consumerRecord, "eventId");

        // 이미 처리된 이벤트이면 skip
        var existingInbox = inboxEventRepository.findByEventId(eventId);
        if (existingInbox.isPresent()) {
            return null;
        }

        createInboxLog(consumerRecord, eventId);
        // 비즈니스 로직 실행
        return joinPoint.proceed();
    }

    private void createInboxLog(ConsumerRecord<String, String> consumerRecord, String eventId) {
        var eventType = getHeaderValueByKey(consumerRecord, "eventType");
        var aggregateType = getHeaderValueByKey(consumerRecord, "aggregateType");
        // 인박스 테이블 저장
        var command = InboxEventCreateCommand.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateType(aggregateType)
                .payload(consumerRecord.value())
                .build();
        inboxEventCommandHandler.handle(command);
    }
}
