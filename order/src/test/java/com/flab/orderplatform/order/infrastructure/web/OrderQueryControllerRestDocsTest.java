package com.flab.orderplatform.order.infrastructure.web;

import com.flab.orderplatform.order.application.OrderQueryService;
import com.flab.orderplatform.order.application.port.out.view.OrderView;
import com.flab.orderplatform.order.support.RestDocsTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.flab.orderplatform.shared.utils.JsonUtils.fromJsonFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RestDocsTest(OrderQueryController.class)
@DisplayName("주문 조회 restDocs 테스트")
public class OrderQueryControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderQueryService orderQueryService;

    @Test
    @DisplayName("주문 조회 성공 응답을 문서화한다.")
    public void searchOrder() throws Exception {
        var response = fromJsonFile("json/search-order.json", OrderView.class);
        given(orderQueryService.searchOrder(any(String.class))).willReturn(response);

        mockMvc.perform(get("/order/{orderNumber}", "20260721-7K3M9QX2WF"))
                .andExpect(status().isOk())
                .andDo(document("order-search-success",
                        pathParameters(
                                parameterWithName("orderNumber").description("조회할 주문번호")
                        ),
                        responseFields(
                                fieldWithPath("success").type(BOOLEAN).description("요청 처리 성공 여부"),
                                fieldWithPath("data.orderNumber").type(STRING).description("주문번호"),
                                fieldWithPath("data.status.code").type(STRING).description("주문 상태 코드"),
                                fieldWithPath("data.status.description").type(STRING).description("주문 상태 설명"),
                                fieldWithPath("data.orderItems[].productName").type(STRING).description("상품명"),
                                fieldWithPath("data.orderItems[].quantity").type(NUMBER).description("주문 수량"),
                                fieldWithPath("data.orderItems[].price").type(NUMBER).description("상품 단가")
                        )
                ));
    }
}
