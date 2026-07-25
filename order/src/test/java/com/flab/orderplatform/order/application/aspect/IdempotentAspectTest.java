package com.flab.orderplatform.order.application.aspect;

import com.flab.orderplatform.order.application.annotation.Idempotent;
import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.common.exception.DuplicatedRequestException;
import com.flab.orderplatform.order.support.RedisTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringJUnitConfig(IdempotentAspectTest.TestConfig.class)
@DisplayName("멱등키 aspect 테스트")
class IdempotentAspectTest {

    private static final String IDEMPOTENT_KEY = "1a2b3c-4d5e6f";
    private static final String REDIS_KEY = "idempotent:" + IDEMPOTENT_KEY;
    /** 만료를 실제로 기다려야 하므로 테스트에서는 짧게 준다. */
    private static final Duration TEST_TTL = Duration.ofSeconds(2);

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private TestOrderService testOrderService;

    @BeforeEach
    void setUp() {
        // 컨테이너를 공유하므로 테스트 간 상태 격리는 직접 한다.
        redissonClient.getKeys().flushall();
        testOrderService.reset();
    }

    @Test
    @DisplayName("[성공] 요청이 한 번만 오면 정상 수행된다.")
    void singleRequestSucceeds() {
        // when
        var result = testOrderService.createOrder(command(IDEMPOTENT_KEY));

        // then
        assertThat(result).isEqualTo("CREATED");
        assertThat(testOrderService.executionCount()).isEqualTo(1);

        // 멱등키는 prefix 를 붙여 저장된다. 아래 값은 실제 Redis 에서 역직렬화해 온 것이다.
        var bucket = redissonClient.getBucket(REDIS_KEY);
        assertThat(bucket.get()).isEqualTo(IdempotentEntry.completed("CREATED"));
    }

    @Test
    @DisplayName("[성공] TTL 이내에 앞선 요청이 완료된 뒤 재요청하면, 로직 재실행 없이 앞선 결과가 반환된다.")
    void completedResultIsReplayedWithinTtl() {
        // given: 첫 요청이 정상 완료되어 결과가 Redis 에 저장된다.
        assertThat(testOrderService.createOrder(command(IDEMPOTENT_KEY))).isEqualTo("CREATED");

        // when: TTL 이 지나기 전에 같은 키로 다시 들어온다.
        var replayed = testOrderService.createOrder(command(IDEMPOTENT_KEY));

        // then: 예외가 아니라 앞선 요청의 결과가 그대로 반환된다.
        assertThat(replayed).isEqualTo("CREATED");
        // 멱등성의 본질 — 같은 값이 나오는 것이 아니라 부수효과가 한 번만 일어나는 것.
        assertThat(testOrderService.executionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("[실패] 앞선 요청이 아직 처리 중이면, 결과를 기다리지 않고 즉시 중복요청예외가 발생한다.")
    void inProgressRequestIsRejected() throws Exception {
        // given: 첫 요청이 대상 로직 안에서 멈춰 있다. Redis 에는 IN_PROGRESS 만 있는 상태다.
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        testOrderService.blockUntilReleased(entered, release);

        var executor = Executors.newSingleThreadExecutor();
        try {
            var firstRequest = executor.submit(() -> testOrderService.createOrder(command(IDEMPOTENT_KEY)));
            assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();

            RBucket<IdempotentEntry> bucket = redissonClient.getBucket(REDIS_KEY);
            assertThat(bucket.get()).isEqualTo(IdempotentEntry.inProgress());

            // when & then: 같은 키의 두 번째 요청은 앞선 요청을 기다리지 않고 즉시 거부된다.
            var duplicatedCommand = command(IDEMPOTENT_KEY);
            assertThatThrownBy(() -> testOrderService.createOrder(duplicatedCommand))
                    .isInstanceOf(DuplicatedRequestException.class)
                    .hasMessageContaining(REDIS_KEY);

            // 두 번째 요청의 대상 메서드는 아예 실행되지 않았고, 첫 요청은 방해받지 않고 완주한다.
            release.countDown();
            assertThat(firstRequest.get(3, TimeUnit.SECONDS)).isEqualTo("CREATED");
            assertThat(testOrderService.executionCount()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("[성공] TTL 이 실제로 만료된 뒤 동일 멱등키로 재요청하면 두 요청 모두 정상 수행된다.")
    void requestAfterTtlExpiredSucceedsAgain() {
        // given
        assertThat(testOrderService.createOrder(command(IDEMPOTENT_KEY))).isEqualTo("CREATED");

        // when: 시계를 조작하는 게 아니라 Redis 가 실제로 키를 만료시킬 때까지 기다린다.
        await().atMost(Duration.ofSeconds(5))
                .until(() -> !redissonClient.getBucket(REDIS_KEY).isExists());

        // then
        assertThat(testOrderService.createOrder(command(IDEMPOTENT_KEY))).isEqualTo("CREATED");
        assertThat(testOrderService.executionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("[성공] 로직이 실패하면 멱등키를 회수해 동일 키로 재시도할 수 있다.")
    void failedRequestReleasesKey() {
        // given: 첫 요청이 예외로 끝난다.
        testOrderService.failNext();
        assertThatThrownBy(() -> testOrderService.createOrder(command(IDEMPOTENT_KEY)))
                .isInstanceOf(IllegalStateException.class);

        // then: 키가 회수되어 있어야 재시도가 가능하다.
        assertThat(redissonClient.getBucket(REDIS_KEY).isExists()).isFalse();
        assertThat(testOrderService.createOrder(command(IDEMPOTENT_KEY))).isEqualTo("CREATED");
        assertThat(testOrderService.executionCount()).isEqualTo(2);
    }

    private OrderCreateCommand command(String idempotentKey) {
        return OrderCreateCommand.builder()
                .idempotentKey(idempotentKey)
                .customerId(100L)
                .orderItems(List.of())
                .build();
    }

    @Configuration
    @EnableAspectJAutoProxy // 본 클래스는 @SpringJUnitConfig(TestConfig.class) 를 사용하므로, @SpringBootApplication 대신 애노테이션이 붙은 클래스를 인식해 자동으로 프록시 객체를 생성해준다.
    static class TestConfig {

        @Bean(destroyMethod = "shutdown")
        RedissonClient redissonClient() {
            var config = new Config();
            config.useSingleServer().setAddress(RedisTestContainer.address());
            return Redisson.create(config);
        }

        @Bean
        IdempotentAspect idempotentAspect(RedissonClient redissonClient) {
            return new IdempotentAspect(redissonClient, TEST_TTL);
        }

        @Bean
        TestOrderService testOrderService() {
            return new TestOrderService();
        }
    }

    /**
     * 포인트컷(com.flab.orderplatform.order.application..*)에 걸리는 위치에 둔 테스트 전용 대상.
     */
    static class TestOrderService {

        private final AtomicInteger executionCount = new AtomicInteger();
        /** 첫 요청이 대상 로직에 진입했음을 알리는 신호 */
        private volatile CountDownLatch entered;
        /** 첫 요청을 계속 진행시키는 신호 */
        private volatile CountDownLatch release;
        private volatile boolean failNext;

        @Idempotent
        public String createOrder(OrderCreateCommand command) {
            executionCount.incrementAndGet();
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("의도적 실패");
            }
            awaitIfBlocked();
            return "CREATED";
        }

        /** release 신호가 올 때까지 로직 안에 머문다 → aspect 입장에서는 "아직 미완료" 상태 */
        private void awaitIfBlocked() {
            if (release == null) {
                return;
            }
            entered.countDown();
            try {
                release.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        void blockUntilReleased(CountDownLatch entered, CountDownLatch release) {
            this.entered = entered;
            this.release = release;
        }

        void failNext() {
            this.failNext = true;
        }

        int executionCount() {
            return executionCount.get();
        }

        void reset() {
            executionCount.set(0);
            entered = null;
            release = null;
            failNext = false;
        }
    }
}
