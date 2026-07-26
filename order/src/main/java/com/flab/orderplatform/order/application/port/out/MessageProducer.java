package com.flab.orderplatform.order.application.port.out;

import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

public interface MessageProducer {
    CompletableFuture<SendResult<String, String>> sendMessage(String topic, String payload);
}
