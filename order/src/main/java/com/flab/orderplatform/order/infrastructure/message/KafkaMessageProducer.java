package com.flab.orderplatform.order.infrastructure.message;

import com.flab.orderplatform.order.application.port.out.MessageProducer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@RequiredArgsConstructor
public class KafkaMessageProducer implements MessageProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public CompletableFuture<SendResult<String, String>> sendMessage(String topic, String messageKey, String message, Map<String, String> headers) {
        var producerRecord = new ProducerRecord<>(topic, messageKey, message);
        if (headers != null) {
            headers.forEach((key, value) -> producerRecord.headers().add(key, value.getBytes(UTF_8)));
        }
        return kafkaTemplate.send(producerRecord);
    }
}
