package com.flab.orderplatform.order.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flab.orderplatform.order.application.OrderCreateFacade;
import com.flab.orderplatform.order.infrastructure.web.common.ApiResponseAdvice;
import com.flab.orderplatform.order.infrastructure.web.common.ErrorCode;
import com.flab.orderplatform.order.infrastructure.web.common.RestExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@Import({OrderCommandController.class, ApiResponseAdvice.class, RestExceptionHandler.class})
@DisplayName("주문 command 컨트롤러 테스트")
public class OrderCommandControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderCreateFacade orderCreateFacade;

    @Test
    @DisplayName("[성공] 주문생성 성공시 200으로 응답한다.")
    public void createOrder() throws Exception {
        given(orderCreateFacade.createOrder(any())).willReturn(1L);

        var request = OrderCreateRequest.builder()
                .customerId(100L)
                .orderItemDtos(List.of(OrderCreateRequest
                        .OrderItemDto
                        .builder()
                        .quantity(3)
                        .name("뽀로로 주스")
                        .productCode("GD10001")
                        .build()))
                .build();
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

    }

    @Test
    @DisplayName("[실패] 주문자 정보가 없으면 400 예외가 발생한다.")
    public void createOrderWithoutCustomerId() throws Exception {
        var request = OrderCreateRequest.builder()
                .customerId(null)
                .orderItemDtos(List.of(OrderCreateRequest
                        .OrderItemDto
                        .builder()
                        .quantity(3)
                        .name("뽀로로 주스")
                        .productCode("GD10001")
                        .build()))
                .build();

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_REQUEST.name()))
                .andExpect(jsonPath("$.error.violations[0].field").value("customerId"));
    }
}