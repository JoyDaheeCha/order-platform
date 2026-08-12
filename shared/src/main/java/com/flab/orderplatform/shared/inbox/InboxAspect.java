package com.flab.orderplatform.shared.inbox;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 인박스 패턴 적용 aspect
 */
@Aspect
@Slf4j
public class InboxAspect {

    private final Map<String, ContextInbox> contextInboxes;

    public InboxAspect(List<ContextInbox> contextInboxes) {
        this.contextInboxes = contextInboxes.stream()
                .collect(Collectors.toUnmodifiableMap(ContextInbox::name, Function.identity()));
    }

    @SuppressWarnings("unchecked")
    private static ConsumerRecord<String, String> getConsumerRecord(ProceedingJoinPoint joinPoint) {
        return Arrays.stream(joinPoint.getArgs())
                .filter(ConsumerRecord.class::isInstance)
                .map(arg -> (ConsumerRecord<String, String>) arg)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("@Inbox 애노테이션은 ConsumerRecord 파라메터가 필요합니다."));
    }

    @Around("@annotation(inbox)")
    public Object recordInboxEvent(ProceedingJoinPoint joinPoint, Inbox inbox) throws Throwable {
        resolve(inbox.value()).receive(getConsumerRecord(joinPoint));
        return joinPoint.proceed();
    }

    private ContextInbox resolve(String contextName) {
        var contextInbox = contextInboxes.get(contextName);
        if (contextInbox == null) {
            throw new IllegalStateException(
                    "등록되지 않은 인박스 컨텍스트입니다. (@Inbox value=%s, 등록된 컨텍스트=%s)"
                            .formatted(contextName, contextInboxes.keySet()));
        }
        return contextInbox;
    }
}
