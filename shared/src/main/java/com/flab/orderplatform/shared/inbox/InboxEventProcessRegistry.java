package com.flab.orderplatform.shared.inbox;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

class InboxEventProcessRegistry {

    private final Map<String, InboxEventProcessor> processors;

    InboxEventProcessRegistry(List<? extends InboxEventProcessor> processors) {
        this.processors = processors.stream()
                .collect(Collectors.toMap(InboxEventProcessor::supportedEventType, Function.identity()));
    }

    Optional<InboxEventProcessor> find(String eventType) {
        return Optional.ofNullable(processors.get(eventType));
    }
}
