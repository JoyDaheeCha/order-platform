package com.flab.orderplatform.inventory;

import com.flab.orderplatform.inventory.application.command.InventoryDecreaseCommand;
import com.flab.orderplatform.inventory.application.port.out.InventoryDecreaseCommandHandler;
import com.flab.orderplatform.inventory.domain.Inventory;
import com.flab.orderplatform.inventory.infrastructure.persistence.InventoryJpaRepository;
import com.flab.orderplatform.persistence.MySqlTestContainer;
import com.flab.orderplatform.persistence.RedisTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
@DisplayName("재고 감소 락 전략 3종 동시성 증명 테스트")
class InventoryDecreaseLockConcurrencyTest {

    private static final int INITIAL_STOCK = 1_000;
    @Autowired
    private InventoryJpaRepository inventoryJpaRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        MySqlTestContainer.registerDataSources(registry);
        RedisTestContainer.registerDataSource(registry);
    }

    /**
     * {@code concurrency} 개의 스레드가 동시에(같은 순간에 출발) 같은 상품코드의 재고를 1개씩 감소시킨다.
     */
    private ConcurrencyResult decreaseConcurrently(
            InventoryDecreaseCommandHandler handler, String productCode, int concurrency) throws InterruptedException {
        var pool = Executors.newFixedThreadPool(concurrency);
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(concurrency);
        var failures = new CopyOnWriteArrayList<Throwable>();

        for (int i = 0; i < concurrency; i++) {
            var orderNumber = "concurrency-order-%d".formatted(i);
            pool.submit(() -> {
                try {
                    start.await();
                    handler.handle(List.of(decreaseCommand(orderNumber, productCode, 1)));
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

        return new ConcurrencyResult(completed, failures);
    }

    private InventoryDecreaseCommand decreaseCommand(String orderNumber, String productCode, int quantity) {
        return InventoryDecreaseCommand.builder()
                .orderNumber(orderNumber)
                .product(InventoryDecreaseCommand.ProductDto.builder()
                        .productCode(productCode)
                        .quantityToDecrease(quantity)
                        .build())
                .build();
    }

    private void seedInventory(String productCode, int stock) {
        inventoryJpaRepository.findByProductCode(productCode)
                .ifPresent(inventoryJpaRepository::delete);
        inventoryJpaRepository.flush();
        inventoryJpaRepository.save(Inventory.builder()
                .productCode(productCode)
                .stock(stock)
                .inventoryHistories(new ArrayList<>())
                .build());
    }

    private record ConcurrencyResult(boolean completedInTime, List<Throwable> failures) {
    }

    @Nested
    @DisplayName("비관락 (SELECT ... FOR UPDATE)")
    class PessimisticLock {

        private static final String PRODUCT_CODE = "CONCURRENCY-PESSIMISTIC";
        private static final int CONCURRENCY = 10;

        @Autowired
        @Qualifier("pessimisticLockInventoryDecreaseCommandHandler")
        private InventoryDecreaseCommandHandler handler;

        @BeforeEach
        void setUp() {
            seedInventory(PRODUCT_CODE, INITIAL_STOCK);
        }

        @Test
        @DisplayName("[증명] 동시 요청 10건이 행 잠금으로 직렬화되어 예외 없이 모두 반영된다")
        void noLostUpdateUnderConcurrency() throws InterruptedException {
            var result = decreaseConcurrently(handler, PRODUCT_CODE, CONCURRENCY);

            assertThat(result.completedInTime()).as("전체 스레드가 제한시간 내 완료").isTrue();
            assertThat(result.failures())
                    .as("비관락은 뒤에 온 트랜잭션을 블로킹하여 대기시키므로, 예외 없이 전원 성공해야 한다")
                    .isEmpty();

            var inventory = inventoryJpaRepository.findByProductCode(PRODUCT_CODE).orElseThrow();
            assertThat(inventory.getStock()).isEqualTo(INITIAL_STOCK - CONCURRENCY);
            assertThat(inventory.getVersion()).isEqualTo(CONCURRENCY); // 성공한 UPDATE 횟수 = 버전 증가 횟수
        }
    }

    @Nested
    @DisplayName("낙관락 (@Version + @Retryable)")
    class OptimisticLock {

        private static final String PRODUCT_CODE = "CONCURRENCY-OPTIMISTIC";
        /**
         * 낙관락은 동시성이 지나치게 높으면 재시도를 소진한 스레드가 실패할 수 있다
         * 따라서 테스트를 안정적으로 관찰하기 위해 비관락/분산락보다 낮은 동시성을 사용한다.
         */
        private static final int CONCURRENCY = 5;

        @Autowired
        @Qualifier("optimisticLockInventoryDecreaseCommandHandler")
        private InventoryDecreaseCommandHandler handler;

        @BeforeEach
        void setUp() {
            seedInventory(PRODUCT_CODE, INITIAL_STOCK);
        }

        @Test
        @DisplayName("[증명] 동시 요청 5건이 버전 충돌 시 재시도되어 예외 없이 모두 반영된다")
        void noLostUpdateUnderConcurrency() throws InterruptedException {
            var result = decreaseConcurrently(handler, PRODUCT_CODE, CONCURRENCY);

            assertThat(result.completedInTime()).as("전체 스레드가 제한시간 내 완료").isTrue();
            assertThat(result.failures())
                    .as("버전 충돌이 나더라도 @Retryable 이 재조회 후 재시도하므로 예외 없이 전원 성공해야 한다")
                    .isEmpty();

            var inventory = inventoryJpaRepository.findByProductCode(PRODUCT_CODE).orElseThrow();
            assertThat(inventory.getStock()).isEqualTo(INITIAL_STOCK - CONCURRENCY);
            assertThat(inventory.getVersion()).isEqualTo(CONCURRENCY);
        }
    }

    @Nested
    @DisplayName("분산락 (Redisson)")
    class DistributedLock {

        private static final String PRODUCT_CODE = "CONCURRENCY-DISTRIBUTED";
        private static final int CONCURRENCY = 10;

        @Autowired
        @Qualifier("distributedLockInventoryDecreaseCommandHandler")
        private InventoryDecreaseCommandHandler handler;

        @BeforeEach
        void setUp() {
            seedInventory(PRODUCT_CODE, INITIAL_STOCK);
        }

        @Test
        @DisplayName("[증명] 동시 요청 10건이 분산락으로 직렬화되어 예외 없이 모두 반영된다")
        void noLostUpdateUnderConcurrency() throws InterruptedException {
            var result = decreaseConcurrently(handler, PRODUCT_CODE, CONCURRENCY);

            assertThat(result.completedInTime()).as("전체 스레드가 제한시간 내 완료").isTrue();
            assertThat(result.failures())
                    .as("분산락이 같은 상품코드에 대해 직렬화한다면, DB에는 매번 최신 버전으로만 접근하므로 " +
                            "버전 충돌 예외(ObjectOptimisticLockingFailureException)가 발생하면 안 된다")
                    .isEmpty();

            var inventory = inventoryJpaRepository.findByProductCode(PRODUCT_CODE).orElseThrow();
            assertThat(inventory.getStock()).isEqualTo(INITIAL_STOCK - CONCURRENCY);
            assertThat(inventory.getVersion()).isEqualTo(CONCURRENCY);
        }
    }
}
