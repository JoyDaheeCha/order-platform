package com.flab.orderplatform.inventory.application;

import com.flab.orderplatform.inventory.application.port.out.InventoryDecreaseCommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class InventoryDecreaseLockResolver {
    private final Map<String, InventoryDecreaseCommandHandler> handler;

    @Value("${inventory.lock-strategy:pessimistic}")
    private String strategy;

    public InventoryDecreaseCommandHandler resolve() {
        return handler.get(strategy + "LockInventoryDecreaseCommandHandler");
    }
}
