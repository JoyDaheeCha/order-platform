package com.flab.orderplatform.inventory.application.lock;

import com.flab.orderplatform.inventory.application.command.InventoryDecreaseCommand;
import com.flab.orderplatform.inventory.application.exception.InventoryDecreasementFailureByConcurrencyException;
import com.flab.orderplatform.inventory.application.port.out.InventoryDecreaseCommandHandler;
import com.flab.orderplatform.inventory.domain.Inventory;
import com.flab.orderplatform.inventory.infrastructure.config.InventoryRetryConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig({InventoryRetryConfig.class, OptimisticLockInventoryDecreaseCommandHandler.class})
@DisplayName("낙관락 재고 감소 핸들러 - 재시도/복구 테스트")
class OptimisticLockInventoryDecreaseCommandHandlerTest {

    private static final String ORDER_NUMBER = "order-1";
    private static final String PRODUCT_CODE = "GD10001";

    @MockitoBean
    private InventoryDecreaseTransactionalWorker worker;

    @Autowired
    private InventoryDecreaseCommandHandler handler;

    @Test
    @DisplayName("[성공] 버전 충돌이 없으면 재시도 없이 1회로 성공한다.")
    void succeedsOnFirstAttemptWithoutRetry() {
        // given
        var expected = List.of(inventory());
        given(worker.handle(anyList())).willReturn(expected);

        // when
        var result = handler.handle(commands());

        // then
        assertThat(result).isEqualTo(expected);
        verify(worker, times(1)).handle(anyList());
    }

    @Test
    @DisplayName("[성공] 버전 충돌(ObjectOptimisticLockingFailureException)이 나면 재시도하여 성공한다.")
    void retriesOnVersionConflictAndSucceeds() {
        // given: 1번째 시도는 다른 스레드와의 버전 충돌로 실패, 2번째 시도(재조회 후 재계산)는 성공
        var expected = List.of(inventory());
        given(worker.handle(anyList()))
                .willThrow(new ObjectOptimisticLockingFailureException(Inventory.class, 1L))
                .willReturn(expected);

        // when
        var result = handler.handle(commands());

        // then
        assertThat(result).isEqualTo(expected);
        verify(worker, times(2)).handle(anyList());
    }

    @Test
    @DisplayName("[실패] 재시도 5회(최초 1회 + 재시도 4회)를 모두 소진하면 동시성 예외로 변환되어 던져진다.")
    void throwsConcurrencyExceptionWhenRetriesExhausted() {
        // given: 매 시도마다 버전 충돌
        given(worker.handle(anyList()))
                .willThrow(new ObjectOptimisticLockingFailureException(Inventory.class, 1L));

        var commands = commands();

        // when & then: @Recover 가 도메인 예외(InventoryDecreasementFailureByConcurrencyException)로 변환한다.
        assertThatThrownBy(() -> handler.handle(commands))
                .isInstanceOf(InventoryDecreasementFailureByConcurrencyException.class)
                .hasMessageContaining(ORDER_NUMBER);

        // @Retryable(maxAttempts = 5) → 총 5번 호출되어야 한다.
        verify(worker, times(5)).handle(anyList());
    }

    private List<InventoryDecreaseCommand> commands() {
        return List.of(InventoryDecreaseCommand.builder()
                .orderNumber(ORDER_NUMBER)
                .product(InventoryDecreaseCommand.ProductDto.builder()
                        .productCode(PRODUCT_CODE)
                        .quantityToDecrease(1)
                        .build())
                .build());
    }

    private Inventory inventory() {
        return Inventory.builder()
                .productCode(PRODUCT_CODE)
                .stock(99)
                .build();
    }
}
