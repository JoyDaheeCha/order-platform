package com.flab.orderplatform.inventory.application;

import com.flab.orderplatform.inventory.application.annotation.InventoryTransactional;
import com.flab.orderplatform.inventory.application.command.InventoryDecreaseCommand;
import com.flab.orderplatform.inventory.application.exception.InventoryNotFoundException;
import com.flab.orderplatform.inventory.application.port.out.InventoryRepository;
import com.flab.orderplatform.inventory.domain.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryCommandHandler {
    private final InventoryRepository inventoryRepository;

    // TODO: 동시성 방어 로직 추가
    @InventoryTransactional
    public Inventory handle(InventoryDecreaseCommand command) {
        var productCode = command.product().productCode();
        var inventory = inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new InventoryNotFoundException(productCode));
        var decreasedStock = command.decreaseStock(inventory);
        return inventoryRepository.save(decreasedStock);
    }
}
