package com.flab.orderplatform.inventory.application;

import com.flab.orderplatform.inventory.application.command.InventoryDecreaseCommand;
import com.flab.orderplatform.inventory.application.command.InventoryReserveCommand;
import com.flab.orderplatform.inventory.application.command.InventoryReservedRestoreCommand;
import com.flab.orderplatform.inventory.application.command.InventoryRestoreCommand;
import com.flab.orderplatform.inventory.application.exception.InventoryNotFoundException;
import com.flab.orderplatform.inventory.application.port.out.InventoryRepository;
import com.flab.orderplatform.inventory.domain.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryCommandHandler {
    private final InventoryRepository inventoryRepository;

    /**
     * 재고 감소 시켜라
     *
     * @param command 재고 감소 명령
     * @return 재고
     */
    public Inventory handle(InventoryDecreaseCommand command) {
        var productCode = command.product().productCode();
        var inventory = inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new InventoryNotFoundException(productCode));
        return command.decreaseInventory(inventory);
    }

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

    /**
     * 재고 선점 원복하라
     *
     * @param command 재고 선점 명령
     * @return 재고
     */
    public Inventory handle(InventoryReservedRestoreCommand command) {
        var productCode = command.product().productCode();
        var inventory = inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new InventoryNotFoundException(productCode));
        return command.restore(inventory);
    }


    /**
     * 가용 재고 원복하라
     *
     * @param command 재고 선점 명령
     * @return 재고
     */
    public Inventory handle(InventoryRestoreCommand command) {
        var productCode = command.product().productCode();
        var inventory = inventoryRepository.findByProductCode(productCode)
                .orElseThrow(() -> new InventoryNotFoundException(productCode));
        return command.restore(inventory);
    }
}
