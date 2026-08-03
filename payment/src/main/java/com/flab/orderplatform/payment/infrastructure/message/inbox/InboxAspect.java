package com.flab.orderplatform.payment.infrastructure.message.inbox;

import com.flab.orderplatform.payment.application.InboxEventCommandHandler;
import com.flab.orderplatform.payment.application.annotation.Inbox;
import com.flab.orderplatform.payment.application.command.InboxEventCreateCommand;
import com.flab.orderplatform.payment.application.port.out.InboxEventRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.flab.orderplatform.shared.event.EventConstants.Headers.*;

/**
 * 인박스 패턴 적용 aspect
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class InboxAspect {
    private final InboxEventCommandHandler inboxEventCommandHandler;
    private final InboxEventRepository inboxEventRepository;

    @SuppressWarnings("unchecked")
    private static @NonNull ConsumerRecord<String, String> getConsumerRecord(ProceedingJoinPoint joinPoint) {
        return Arrays.stream(joinPoint.getArgs())
                .filter(ConsumerRecord.class::isInstance)
                .map(arg -> (ConsumerRecord<String, String>) arg)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("@Inbox 애노테이션은 ConsumerRecord 파라메터가 필요합니다."));
    }

    private void createInboxLog(ConsumerRecord<String, String> consumerRecord, String eventId) {
        var eventType = getHeaderValueByKey(consumerRecord, EVENT_TYPE);
        var aggregateType = getHeaderValueByKey(consumerRecord, AGGREGATE_TYPE);
        // 인박스 테이블 저장
        var command = InboxEventCreateCommand.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateType(aggregateType)
                .payload(consumerRecord.value())
                .build();
        inboxEventCommandHandler.handle(command);
    }

    private static String getHeaderValueByKey(ConsumerRecord<String, String> consumerRecord, String headerKey) {
        var header = consumerRecord.headers().lastHeader(headerKey);
        if (header == null) {
            throw new IllegalStateException("필수 헤더(%s)가 없습니다.".formatted(headerKey));
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    @Around("@annotation(inbox)")
    public void handle(ProceedingJoinPoint joinPoint, Inbox inbox) {

        var consumerRecord = getConsumerRecord(joinPoint);
        var eventId = getHeaderValueByKey(consumerRecord, EVENT_ID);

        // 이미 처리된 이벤트이면 skip
        var existingInbox = inboxEventRepository.findByEventId(eventId);
        if (existingInbox.isPresent()) {
            return;
        }

        // 인박스 데이터 생성
        try {
            createInboxLog(consumerRecord, eventId);
        } catch (DataIntegrityViolationException e) {
            // 이미 처리된 메시지일 경우 unique 제약 조건에 걸릴 수 있다. 따라서 이미처리됨으로 간주하고 무시한다.
        }
    }
}
