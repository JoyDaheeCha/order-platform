package com.flab.orderplatform.inventory.application;

import com.flab.orderplatform.inventory.application.port.out.InventoryDecreaseCommandHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class InventoryDecreaseLockResolver {
    private final Map<String, InventoryDecreaseCommandHandler> handler;

    @Value("${inventory.lock-strategy:distributed}")
    private String strategy;

    @PostConstruct
    void validate() {
        if (!handler.containsKey(beanName())) {
            throw new IllegalStateException(
                    "지원하지 않는 락 전략입니다: %s (가능: %s)".formatted(strategy, handler.keySet()));
        }
    }

    public InventoryDecreaseCommandHandler resolve() {
        return handler.get(beanName());
    }

    private String beanName() {
        return strategy + "LockInventoryDecreaseCommandHandler";
    }
}
