package com.flab.orderplatform.payment.application;

import com.flab.orderplatform.payment.application.annotation.PaymentTransactional;
import com.flab.orderplatform.payment.application.command.InboxEventCreateCommand;
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
}
