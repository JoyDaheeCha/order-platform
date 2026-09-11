package com.flab.orderplatform.inventory;

import com.flab.orderplatform.inventory.application.InventoryFacade;
import com.flab.orderplatform.inventory.domain.Inventory;
import com.flab.orderplatform.inventory.infrastructure.persistence.InventoryJpaRepository;
import com.flab.orderplatform.persistence.MySqlTestContainer;
import com.flab.orderplatform.persistence.RedisTestContainer;
import com.flab.orderplatform.shared.event.OrderPaidPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("재고 감소 분산락 테스트")
class InventoryDecreaseLockConcurrencyTest {

    private static final int INITIAL_STOCK = 1_000;
    private static final String PRODUCT_CODE = "CONCURRENCY-DISTRIBUTED";
    private static final int CONCURRENCY = 10;

    @Autowired
    InventoryFacade inventoryFacade;

    @Autowired
    private InventoryJpaRepository inventoryJpaRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        MySqlTestContainer.registerDataSources(registry);
        RedisTestContainer.registerDataSource(registry);
    }

    @BeforeEach
    void setUp() {
        inventoryJpaRepository.findByProductCode(InventoryDecreaseLockConcurrencyTest.PRODUCT_CODE)
                .ifPresent(inventoryJpaRepository::delete);
        inventoryJpaRepository.flush();
        inventoryJpaRepository.save(Inventory.builder()
                .productCode(InventoryDecreaseLockConcurrencyTest.PRODUCT_CODE)
                .stock(InventoryDecreaseLockConcurrencyTest.INITIAL_STOCK)
                .inventoryHistories(new ArrayList<>())
                .build());
    }

    @Test
    @DisplayName("[증명] 동시 요청 10건이 분산락으로 직렬화되어 예외 없이 모두 반영된다")
    void noLostUpdateUnderConcurrency() throws InterruptedException {
        // given
        var pool = Executors.newFixedThreadPool(InventoryDecreaseLockConcurrencyTest.CONCURRENCY);
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(InventoryDecreaseLockConcurrencyTest.CONCURRENCY);
        var failures = new CopyOnWriteArrayList<Throwable>();

        // when
        for (int i = 0; i < InventoryDecreaseLockConcurrencyTest.CONCURRENCY; i++) {
            var orderNumber = "concurrency-order-%d".formatted(i);
            pool.submit(() -> {
                try {
                    start.await();
                    inventoryFacade.decreaseStock(
                            new OrderPaidPayload(orderNumber,
                                    List.of(new OrderPaidPayload.OrderItemDto(InventoryDecreaseLockConcurrencyTest.PRODUCT_CODE, 1))));
                } catch (Throwable t) {
                    // AssertJ의 리스트 출력은 스택트레이스를 일부만 보여주므로, 실패 원인을 바로 확인할 수 있게 전체를 출력한다.
                    t.printStackTrace();
                    failures.add(t);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown(); // 모든 스레드를 동시에 출발시켜 충돌 가능성을 최대화한다.
        boolean completed = done.await(60, TimeUnit.SECONDS);
        pool.shutdown();

        // then
        var result = new ConcurrencyResult(completed, failures);
        assertThat(result.completedInTime()).as("전체 스레드가 제한시간 내 완료").isTrue();
        var inventory = inventoryJpaRepository.findByProductCode(PRODUCT_CODE).orElseThrow();
        assertThat(inventory.getStock()).isEqualTo(INITIAL_STOCK - CONCURRENCY);
    }

    private record ConcurrencyResult(
            boolean completedInTime,
            List<Throwable> failures
    ) {
    }
}
