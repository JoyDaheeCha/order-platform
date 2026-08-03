package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.application.port.out.OrderRepository;
import com.flab.orderplatform.order.domain.external.Product;
import com.flab.orderplatform.order.infrastructure.config.OrderDomainConfig;
import com.flab.orderplatform.order.infrastructure.persistence.OrderPersistenceConfig;
import com.flab.orderplatform.order.infrastructure.persistence.OrderRepositoryAdaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DataJpaTest
@Import({
        OrderPersistenceConfig.class, OrderDomainConfig.class,
        OrderRepositoryAdaptor.class})
@DisplayName("주문 커멘드 핸들러 테스트")
class OrderCommandHandlerTest {

    // 테스트 데이터 저장용
    @Autowired
    private OrderRepository orderRepository;
    private OrderCommandHandler orderCommandHandler;

    @Autowired
    private TestEntityManager em;

    @MockitoBean
    ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        this.orderCommandHandler = new OrderCommandHandler(orderRepository, eventPublisher);
    }

    @Test
    @DisplayName("주문 생성시 주문 상품이 생성된다.")
    void createOrderTest() {
        // given
        var command = OrderCreateCommand.builder()
                .customerId(1L)
                .orderItems(List.of(
                        new OrderCreateCommand.OrderItemDto(2, "뽀로로주스사과맛", "GD10001"),
                        new OrderCreateCommand.OrderItemDto(2, "뽀로로주스사과맛", "GD10001")
                ))
                .idempotentKey("idempotent-key-1234")
                .build();
        var productMap = Map.of(
                "GD10001", new Product(1L, "GD10001", 1000L),
                "GD10002", new Product(2L, "GD10002", 2000L)
        );
        // when
        var order = orderCommandHandler.handle("order-number", productMap, command);

        em.flush(); // order insert 한번, ordeItem 인서트 2번 눈으로 확인. (일대다 -> 양방향 변경후 update 쿼리 사라짐 확인 완료)
        em.clear();
        // then
        assertSoftly(softly -> {
            softly.assertThat(order.getOrderNumber()).isNotEmpty();
            softly.assertThat(order.getOrderItems()).hasSize(2);
        });
    }
}