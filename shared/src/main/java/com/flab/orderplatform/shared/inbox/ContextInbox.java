package com.flab.orderplatform.shared.inbox;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

public class ContextInbox {

    private final String name;
    private final InboxEventRecorder recorder;
    private final InboxEventService service;

    public ContextInbox(String name,
                        InboxEventJpaRepository jpaRepository,
                        PlatformTransactionManager transactionManager,
                        List<? extends InboxEventProcessor> processors) {

        var repository = new InboxEventRepositoryAdaptor(jpaRepository);
        var commandHandler = new InboxEventCommandHandler(repository, new TransactionTemplate(transactionManager));

        this.name = name;
        this.recorder = new InboxEventRecorder(commandHandler, repository);
        this.service = new InboxEventService(repository, commandHandler, new InboxEventProcessRegistry(processors));
    }

    String name() {
        return name;
    }

    void receive(ConsumerRecord<String, String> consumerRecord) {
        recorder.record(consumerRecord);
    }

    public void processInboxEvents() {
        service.processInboxEvents();
    }
}
