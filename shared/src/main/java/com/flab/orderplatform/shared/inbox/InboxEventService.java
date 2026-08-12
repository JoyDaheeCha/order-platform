package com.flab.orderplatform.shared.inbox;

import com.flab.orderplatform.shared.inbox.command.InboxEventFailCommand;
import com.flab.orderplatform.shared.inbox.command.InboxEventSucceedCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static com.flab.orderplatform.shared.inbox.InboxEventStatus.CREATED;
import static com.flab.orderplatform.shared.inbox.InboxEventStatus.FAILED;

/**
 * 인박스 이벤트 처리 스케줄러
 */
@Slf4j
@RequiredArgsConstructor
class InboxEventService {

    /**
     * 로직 실행이 필요한 인박스 상태: 생성완료, 실패 (실패한 로직은 재처리 필요)
     */
    public static final List<InboxEventStatus> INBOX_STATUSES_TO_PROCESS = List.of(CREATED, FAILED);
    private static final PageRequest DEFAULT_PAGE_REQUEST = PageRequest.of(0, 500, Sort.by("id").ascending());

    private final InboxEventRepository inboxEventRepository;
    private final InboxEventCommandHandler inboxEventCommandHandler;
    private final InboxEventProcessRegistry inboxEventProcessRegistry;

    void processInboxEvents() {
        var inboxEvents = inboxEventRepository.findByStatusIn(INBOX_STATUSES_TO_PROCESS, DEFAULT_PAGE_REQUEST);

        inboxEvents.forEach(event -> {
            var searchedProcessor = inboxEventProcessRegistry.find(event.getEventType());
            if (searchedProcessor.isEmpty()) {
                log.error("이벤트 타입 {}에 대한 이벤트 프로세서가 미등록되어있습니다. 프로세서를 등록하세요.", event.getEventType());
                return;
            }
            var processor = searchedProcessor.get();
            // 비즈니스 로직 진행 & 인박스 처리 상태 업데이트
            var eventId = event.getEventId();
            try {
                processor.process(event); // 트랜잭션이 따로여서 문제
                inboxEventCommandHandler.handle(new InboxEventSucceedCommand(eventId));
            } catch (Exception e) {
                log.error("인박스 이벤트 처리 실패 (eventId = {})", eventId, e);
                try {
                    inboxEventCommandHandler.handle(new InboxEventFailCommand(eventId));
                } catch (Exception ex) {
                    log.error("인박스 실패 상태 갱신 실패 (eventId={})", eventId, ex);
                }
            }
        });
    }
}
