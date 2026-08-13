package com.flab.orderplatform.inventory.application.lock;

import com.flab.orderplatform.inventory.application.annotation.DistributedLock;
import com.flab.orderplatform.inventory.application.command.InventoryDecreaseCommand;
import com.flab.orderplatform.inventory.application.exception.InventoryDecreasementFailureByConcurrencyException;
import com.flab.orderplatform.inventory.application.port.out.InventoryDecreaseCommandHandler;
import com.flab.orderplatform.inventory.domain.Inventory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 재고감소 커멘드 핸들러 (분산락 사용)
 */
@Slf4j
@Service("distributedLockInventoryDecreaseCommandHandler")
@RequiredArgsConstructor
public class DistributedLockInventoryDecreaseCommandHandler implements InventoryDecreaseCommandHandler {
    private final InventoryDecreaseTransactionalWorker worker;

    /**
     * 상품 코드를 기반으로 분산락 키 생성
     */
    @Override
    @DistributedLock(key = "#commands.![product.productCode]", fallback = "handleFallback")
    public List<Inventory> handle(List<InventoryDecreaseCommand> commands) {
        return worker.handle(commands);
    }

    /**
     * 분산락 획득 실패시 실행되는 fallback 메서드
     */
    @SuppressWarnings("unused")
    public String handleFallback(List<InventoryDecreaseCommand> commands) {
        var orderNumber = commands.getFirst().orderNumber();
        log.error("동시성 충돌로 인해 재고 감소에 실패했습니다. (주문번호:{})", orderNumber);
        throw new InventoryDecreasementFailureByConcurrencyException(orderNumber);
    }
}
