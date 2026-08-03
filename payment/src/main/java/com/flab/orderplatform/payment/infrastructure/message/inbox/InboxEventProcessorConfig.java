package com.flab.orderplatform.payment.infrastructure.message.inbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class InboxEventProcessorConfig {

    @Bean
    public Map<String, InboxEventProcessor> processorByEventType(List<InboxEventProcessor> processors) {
        return processors.stream()
                .collect(Collectors.toMap(InboxEventProcessor::supportedEventType, Function.identity()));
    }
}
