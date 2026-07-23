package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.application.exception.SystemException;
import com.flab.orderplatform.order.application.port.out.ProductRepository;
import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.OrderNumberGenerator;
import com.flab.orderplatform.order.domain.external.Product;
import com.flab.orderplatform.order.infrastructure.config.RetryConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringJUnitConfig({RetryConfig.class, OrderCreateFacade.class})
@DisplayName("주문 생성 facade 테스트")
class OrderCreateFacadeRetryTest {

    private static final String PRODUCT_CODE = "GD10001";
    @MockitoBean
    private OrderCommandHandler orderCommandHandler;
    @MockitoBean
    private OrderNumberGenerator orderNumberGenerator;
    @MockitoBean
    private ProductRepository productRepository;
    @Autowired
    private OrderCreateFacade orderCreateFacade;

    @Test
    @DisplayName("[성공] 주문번호가 중복되면 새 번호로 재시도하여 성공한다.")
    void retriesWithNewOrderNumberAndSucceeds() {
        // given: 첫 시도는 중복 예외, 두 번째 시도는 성공
        givenProductExists();
        given(orderNumberGenerator.generate()).willReturn("20260723-AAAAAAAAAA", "20260723-BBBBBBBBBB");

        var savedOrder = mock(Order.class);
        given(savedOrder.getId()).willReturn(1L);
        given(orderCommandHandler.handle(any(), any(), any()))
                .willThrow(new DuplicateKeyException("duplicate order_number")) // 1번째 시도에 중복키 예외 발생
                .willReturn(savedOrder); // 두번째 시도에 정상 저장

        // when
        var orderId = orderCreateFacade.createOrder(command());

        // then
        // 재시도후 저장에 성공하엿다.
        assertThat(orderId).isEqualTo(1L);
        // 주문번호 저장이 2번 시도되었다.
        var orderNumberCaptor = ArgumentCaptor.forClass(String.class);
        verify(orderCommandHandler, times(2)).handle(orderNumberCaptor.capture(), any(), any());
        assertThat(orderNumberCaptor.getAllValues())
                .containsExactly("20260723-AAAAAAAAAA", "20260723-BBBBBBBBBB")
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("[성공] 주문번호 충돌이 없으면 재시도 없이 1회로 성공한다.")
    void succeedsOnFirstAttemptWithoutRetry() {
        // given
        givenProductExists();
        given(orderNumberGenerator.generate()).willReturn("20260723-AAAAAAAAAA");
        var savedOrder = mock(Order.class);
        given(savedOrder.getId()).willReturn(1L);
        given(orderCommandHandler.handle(any(), any(), any())).willReturn(savedOrder);

        // when
        var orderId = orderCreateFacade.createOrder(command());

        // then
        assertThat(orderId).isEqualTo(1L);
        verify(orderCommandHandler, times(1)).handle(any(), any(), any());
    }

    @Test
    @DisplayName("[실패] 재시도(최초 1회 + 재시도 2회)를 모두 소진하면 SystemException 을 던진다.")
    void throwsSystemExceptionWhenRetriesExhausted() {
        // given: 매번 중복 예외 발생
        givenProductExists();
        given(orderNumberGenerator.generate()).willReturn("20260723-AAAAAAAAA1", "20260723-AAAAAAAAA2", "20260723-AAAAAAAAA3");
        given(orderCommandHandler.handle(any(), any(), any()))
                .willThrow(new DuplicateKeyException("duplicate order_number"));

        var command = command();

        // when & then: recover 가 원인 예외를 담아 SystemException 으로 변환
        assertThatThrownBy(() -> orderCreateFacade.createOrder(command))
                .isInstanceOf(SystemException.class)
                .hasCauseInstanceOf(DuplicateKeyException.class);

        // 최초 1회 + 재시도 2회 = 총 3회 (@Retryable 기본 maxAttempts = 3)
        verify(orderCommandHandler, times(3)).handle(any(), any(), any());
    }

    private OrderCreateCommand command() {
        return OrderCreateCommand.builder()
                .customerId(100L)
                .orderItems(List.of(
                        OrderCreateCommand.OrderItemDto.builder()
                                .quantity(3)
                                .name("뽀로로 주스")
                                .productCode(PRODUCT_CODE)
                                .build()))
                .build();
    }

    private void givenProductExists() {
        var product = Product.builder()
                .productCode(PRODUCT_CODE)
                .price(100L)
                .build();
        given(productRepository.findAllByProductCodeIn(anyList()))
                .willReturn(List.of(product));
    }
}
