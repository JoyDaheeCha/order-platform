package com.flab.orderplatform.inventory.application.port.out;

import com.flab.orderplatform.inventory.application.command.InventoryDecreaseCommand;
import com.flab.orderplatform.inventory.domain.Inventory;

import java.util.List;

/**
 * 재고 감소 전용 커멘드 핸들러
 * <p>
 *     낙관락, 비관락등 다양한 방법으로 재고 감소를 구현하기 위해 interface로 정의합니다.
 * </p>
 */
public interface InventoryDecreaseCommandHandler {
    /**
     * 재고 감소 시켜라
     */
    List<Inventory> handle(List<InventoryDecreaseCommand> commands);
}
