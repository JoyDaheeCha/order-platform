package com.flab.orderplatform.order.application.port.out;

import org.springframework.kafka.support.SendResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface MessageProducer {
    CompletableFuture<SendResult<String, String>> sendMessage(String topic, String messageKey, String message, Map<String, String> headers);
}
