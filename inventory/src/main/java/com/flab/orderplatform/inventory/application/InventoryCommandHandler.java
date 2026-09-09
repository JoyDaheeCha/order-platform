package com.flab.orderplatform.inventory.application;

import com.flab.orderplatform.inventory.application.annotation.InventoryTransactional;
import com.flab.orderplatform.inventory.application.command.InventoryDecreaseCommand;
import com.flab.orderplatform.inventory.application.command.InventoryReserveCommand;
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
    /**
     * 재고 감소 시켜라
     *
     * @param command 재고 감소 명령
     * @return 재고
     */
    @InventoryTransactional
    public Inventory handle(InventoryDecreaseCommand command) {
        var productCode = command.product().productCode();
        var inventory = inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new InventoryNotFoundException(productCode));
        return command.decreaseStock(inventory);
    }

    // TODO: 동시성 방어로직 추가

    /**
     * 재고 선점하라
     *
     * @param command 재고 선점 명령
     * @return 재고
     */
    public Inventory handle(InventoryReserveCommand command) {
        var productCode = command.product().productCode();
        var inventory = inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new InventoryNotFoundException(productCode));
        return command.reserve(inventory);
    }
}
