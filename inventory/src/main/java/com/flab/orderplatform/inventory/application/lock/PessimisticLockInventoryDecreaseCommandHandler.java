package com.flab.orderplatform.inventory.application.lock;

import com.flab.orderplatform.inventory.application.annotation.InventoryTransactional;
import com.flab.orderplatform.inventory.application.command.InventoryDecreaseCommand;
import com.flab.orderplatform.inventory.application.port.out.InventoryDecreaseCommandHandler;
import com.flab.orderplatform.inventory.domain.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("pessimisticLockInventoryDecreaseCommandHandler")
@RequiredArgsConstructor
public class PessimisticLockInventoryDecreaseCommandHandler implements InventoryDecreaseCommandHandler {
    private final InventoryDecreaseTransactionalWorker worker;

    /**
     * 특정 주문에 대해 재고를 일괄 감소시킨다.
     */
    @Override
    @InventoryTransactional
    public List<Inventory> handle(List<InventoryDecreaseCommand> commands) {
        return worker.handleWithPessimisticLock(commands);
    }
}
