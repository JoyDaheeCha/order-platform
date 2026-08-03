package com.flab.orderplatform.payment.infrastructure.message.inbox;

import com.flab.orderplatform.payment.application.InboxEventCommandHandler;
import com.flab.orderplatform.payment.application.command.InboxEventFailCommand;
import com.flab.orderplatform.payment.application.command.InboxEventSucceedCommand;
import com.flab.orderplatform.payment.application.port.out.InboxEventRepository;
import com.flab.orderplatform.payment.domain.status.InboxEventStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.flab.orderplatform.payment.domain.status.InboxEventStatus.CREATED;
import static com.flab.orderplatform.payment.domain.status.InboxEventStatus.FAILED;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * 인박스 이벤트 처리 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InboxEventScheduler {

    /**
     * 로직 실행이 필요한 인박스 상태: 생성완료, 실패 (실패한 로직은 재처리 필요)
     */
    public static final List<InboxEventStatus> INBOX_STATUSES_TO_PROCESS = List.of(CREATED, FAILED);
    private static final PageRequest DEFAULT_PAGE_REQUEST = PageRequest.of(0, 500);
    private final InboxEventRepository inboxEventRepository;
    private final InboxEventCommandHandler inboxEventCommandHandler;
    private final Map<String, InboxEventProcessor> processorsByEventType;

    @Scheduled(fixedDelay = 1, timeUnit = MINUTES)
    @SchedulerLock(name = "processInboxEvents", lockAtLeastFor = "10s", lockAtMostFor = "50s")
    void processInboxEvents() {
        var inboxEvents = inboxEventRepository.findByStatusIn(INBOX_STATUSES_TO_PROCESS, DEFAULT_PAGE_REQUEST);

        inboxEvents.forEach(event -> {
            var processor = processorsByEventType.get(event.getEventType());
            if (processor == null) {
                return;
            }
            // 비즈니스 로직 진행 & 인박스 처리 상태 업데이트
            var eventId = event.getEventId();
            try {
                processor.process(event);
                inboxEventCommandHandler.handle(new InboxEventSucceedCommand(eventId));
            } catch (Exception e){
                log.error("인박스 이벤트 처리 실패 (eventId = {})", eventId, e);
                try {
                    inboxEventCommandHandler.handle(new InboxEventFailCommand(eventId));
                } catch (Exception ex){
                    log.error("인박스 실패 상태 갱신 실패 (eventId={})", eventId, ex);
                }
            }
        });
    }
}
