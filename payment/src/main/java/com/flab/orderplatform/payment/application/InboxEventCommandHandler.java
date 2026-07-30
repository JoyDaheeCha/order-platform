package com.flab.orderplatform.payment.application;

import com.flab.orderplatform.payment.application.annotation.PaymentTransactional;
import com.flab.orderplatform.payment.application.command.InboxEventCreateCommand;
import com.flab.orderplatform.payment.application.command.InboxEventFailCommand;
import com.flab.orderplatform.payment.application.command.InboxEventSucceedCommand;
import com.flab.orderplatform.payment.application.exception.InboxEventNotFoundException;
import com.flab.orderplatform.payment.application.port.out.InboxEventRepository;
import com.flab.orderplatform.payment.domain.InboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InboxEventCommandHandler {

    private final InboxEventRepository inboxEventRepository;

    @PaymentTransactional
    public InboxEvent handle(InboxEventCreateCommand command) {
        var inboxEvent = command.create();
        return inboxEventRepository.save(inboxEvent);
    }

    @PaymentTransactional
    public InboxEvent handle(InboxEventSucceedCommand command) {
        var inboxEvent = inboxEventRepository.findByEventId(command.eventId())
                .orElseThrow(() -> new InboxEventNotFoundException(command.eventId()));
        return inboxEventRepository.save(inboxEvent);
    }

    @PaymentTransactional
    public InboxEvent handle(InboxEventFailCommand command) {
        var inboxEvent = inboxEventRepository.findByEventId(command.eventId())
                .orElseThrow(() -> new InboxEventNotFoundException(command.eventId()));
        return inboxEventRepository.save(inboxEvent);
    }
}
