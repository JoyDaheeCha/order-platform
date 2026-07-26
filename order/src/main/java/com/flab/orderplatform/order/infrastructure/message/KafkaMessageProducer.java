package com.flab.orderplatform.order.infrastructure.message;

import com.flab.orderplatform.order.application.port.out.MessageProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class KafkaMessageProducer implements MessageProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public CompletableFuture<SendResult<String, String>> sendMessage(String topic, String message) {
        return kafkaTemplate.send(topic, message);
    }
}
