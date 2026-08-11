package com.flab.orderplatform.shared.inbox;

import com.flab.orderplatform.shared.inbox.command.InboxEventCreateCommand;
import com.flab.orderplatform.shared.inbox.command.InboxEventFailCommand;
import com.flab.orderplatform.shared.inbox.command.InboxEventSucceedCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 인박스 커멘드 핸들러
 * <p>
 * 컨텍스트마다 TransactionManager 가 다르므로 {@code @Transactional} 대신
 * {@link org.springframework.transaction.support.TransactionTemplate} 으로 트랜잭션 경계를 설정한다.
 * </p>
 */
@RequiredArgsConstructor
public class InboxEventCommandHandler {

    private final InboxEventRepository inboxEventRepository;
    private final TransactionTemplate transactionTemplate;

    public InboxEvent handle(InboxEventCreateCommand command) {
        return transactionTemplate.execute(
                status -> {
                    var inboxEvent = command.create();
                    return inboxEventRepository.save(inboxEvent);
                }
        );
    }

    public InboxEvent handle(InboxEventSucceedCommand command) {
        return transactionTemplate.execute(
                status -> {
                    var inboxEvent = findInboxEvent(command.eventId());
                    var updatedInboxEvent = command.succeed(inboxEvent);
                    return inboxEventRepository.save(updatedInboxEvent);
                }
        );
    }

    private InboxEvent findInboxEvent(String eventId) {
        return inboxEventRepository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalStateException("인박스 이벤트가 존재하지 않습니다.(eventId: %s)".formatted(eventId)));
    }

    public InboxEvent handle(InboxEventFailCommand command) {
        return transactionTemplate.execute(
                status -> {
                    var inboxEvent = findInboxEvent(command.eventId());
                    var updatedInboxEvent = command.fail(inboxEvent);
                    return inboxEventRepository.save(updatedInboxEvent);
                }
        );
    }
}
