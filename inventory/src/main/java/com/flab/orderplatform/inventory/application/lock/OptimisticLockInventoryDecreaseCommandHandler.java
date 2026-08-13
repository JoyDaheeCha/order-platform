package com.flab.orderplatform.inventory.application.lock;

import com.flab.orderplatform.inventory.application.command.InventoryDecreaseCommand;
import com.flab.orderplatform.inventory.application.exception.InventoryDecreasementFailureByConcurrencyException;
import com.flab.orderplatform.inventory.application.port.out.InventoryDecreaseCommandHandler;
import com.flab.orderplatform.inventory.domain.Inventory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service("optimisticLockInventoryDecreaseCommandHandler")
@RequiredArgsConstructor
public class OptimisticLockInventoryDecreaseCommandHandler implements InventoryDecreaseCommandHandler {
    private final InventoryDecreaseTransactionalWorker worker;

    /**
     * 실패시 최대 5회 재시도
     * 대기 시간 0.05초 -> 0.1초 -> 0.2초 -> 0.4초 ...로 지수 증가
     */
    @Override
    @Retryable(
            maxAttempts = 5,
            retryFor = ObjectOptimisticLockingFailureException.class,
            backoff = @Backoff(delay = 50, multiplier = 2.0, random = true)
    )
    public List<Inventory> handle(List<InventoryDecreaseCommand> commands) {
        return worker.handle(commands);
    }

    // TODO: MFL 처리
    @SuppressWarnings("unused")
    @Recover
    public List<Inventory> recover(ObjectOptimisticLockingFailureException e, List<InventoryDecreaseCommand> commands) {
        var orderNumber = commands.getFirst().orderNumber();
        log.error("동시성 충돌로 인해 재고 감소에 실패했습니다. (주문번호:{})", orderNumber, e);
        throw new InventoryDecreasementFailureByConcurrencyException(orderNumber);
    }
}
