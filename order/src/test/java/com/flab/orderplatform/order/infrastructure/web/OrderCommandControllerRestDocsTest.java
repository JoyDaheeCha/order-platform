package com.flab.orderplatform.order.infrastructure.web;

import com.flab.orderplatform.order.application.OrderCreateFacade;
import com.flab.orderplatform.order.support.RestDocsTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.flab.orderplatform.shared.utils.JsonUtils.toJson;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RestDocsTest(OrderCommandController.class)
@DisplayName("주문 생성 restDocs 테스트")
public class OrderCommandControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderCreateFacade orderService;

    @Test
    @DisplayName("주문 생성 성공 응답을 문서화한다.")
    public void createOrder() throws Exception {
        var request = OrderCreateRequest.builder()
                .customerId(1L)
                .orderItemDtos(List.of(
                        OrderCreateRequest.OrderItemDto.builder()
                                .quantity(2)
                                .name("뽀로로 보리차")
                                .productCode("PRD-0001")
                                .build()
                ))
                .build();
        given(orderService.createOrder(any())).willReturn(1L);

        mockMvc.perform(post("/order")
                        .header("Idempotency-key", "3f4b6c2a-1234-4a5b-9c8d-abcdef123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andDo(document("order-create-success",
                        requestHeaders(
                                headerWithName("Idempotency-key").description("중복 주문 생성 방지를 위한 멱등키")
                        ),
                        requestFields(
                                fieldWithPath("customerId").type(NUMBER).description("주문자 id"),
                                fieldWithPath("orderItemDtos[].quantity").type(NUMBER).description("주문 수량"),
                                fieldWithPath("orderItemDtos[].name").type(STRING).description("상품명"),
                                fieldWithPath("orderItemDtos[].productCode").type(STRING).description("상품 코드")
                        ),
                        responseFields(
                                fieldWithPath("success").type(BOOLEAN).description("요청 처리 성공 여부"),
                                fieldWithPath("data").type(NUMBER).description("생성된 주문 PK")
                        )
                ));
    }
}
