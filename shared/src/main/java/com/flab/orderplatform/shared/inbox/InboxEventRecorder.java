package com.flab.orderplatform.shared.inbox;

import com.flab.orderplatform.shared.inbox.command.InboxEventCreateCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;

import java.nio.charset.StandardCharsets;

import static com.flab.orderplatform.shared.event.EventConstants.Headers.*;

/**
 * 수신된 메시지를 Inbox에 적재
 */
@Slf4j
@RequiredArgsConstructor
public class InboxEventRecorder {
    private final InboxEventCommandHandler inboxEventCommandHandler;
    private final InboxEventRepository inboxEventRepository;

    private static String getHeaderValueByKey(ConsumerRecord<String, String> consumerRecord, String headerKey) {
        var header = consumerRecord.headers().lastHeader(headerKey);
        if (header == null) {
            throw new IllegalStateException("필수 헤더(%s)가 없습니다.".formatted(headerKey));
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    void record(ConsumerRecord<String, String> consumerRecord) {
        var eventId = getHeaderValueByKey(consumerRecord, EVENT_ID);

        // 이미 적재된 이벤트이면 skip
        if (inboxEventRepository.findByEventId(eventId).isPresent()) {
            return;
        }

        try {
            createInboxLog(consumerRecord, eventId);
        } catch (DataIntegrityViolationException e) {
            // 동일 메시지를 동시에 수신하면 유니크 제약(uk_inbox_event_id)에 걸린다.
            // 이미 적재된 것으로 간주하고 무시한다.
            log.debug("인박스 중복 적재 시도를 무시합니다. (eventId={})", eventId);
        }
    }

    private void createInboxLog(ConsumerRecord<String, String> consumerRecord, String eventId) {
        var command = InboxEventCreateCommand.builder()
                .eventId(eventId)
                .eventType(getHeaderValueByKey(consumerRecord, EVENT_TYPE))
                .aggregateType(getHeaderValueByKey(consumerRecord, AGGREGATE_TYPE))
                .payload(consumerRecord.value())
                .build();

        inboxEventCommandHandler.handle(command);
    }
}
