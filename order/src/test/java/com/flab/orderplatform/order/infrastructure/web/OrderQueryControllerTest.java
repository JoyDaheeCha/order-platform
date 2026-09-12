package com.flab.orderplatform.order.infrastructure.web;

import com.flab.orderplatform.order.application.OrderQueryService;
import com.flab.orderplatform.order.application.port.out.view.OrderView;
import com.flab.orderplatform.order.infrastructure.web.common.ApiResponseAdvice;
import com.flab.orderplatform.order.infrastructure.web.common.RestExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.flab.orderplatform.order.domain.status.OrderStatus.CONFIRMED;
import static com.flab.orderplatform.shared.utils.JsonUtils.fromJsonFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@Import({OrderQueryController.class, ApiResponseAdvice.class, RestExceptionHandler.class})
@DisplayName("주문 쿼리 컨트롤러 테스트")
public class OrderQueryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderQueryService orderQueryService;

    @Test
    @DisplayName("[성공] 주문생성 성공시 200으로 응답한다.")
    public void createOrder() throws Exception {
        var response = fromJsonFile("json/search-order.json", OrderView.class);
        given(orderQueryService.searchOrder(any(String.class))).willReturn(response);

        mockMvc.perform(get("/order/{orderNumber}", "20260721-7K3M9QX2WF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status.code").value(CONFIRMED.name()))
                .andExpect(jsonPath("$.data.status.description").value(CONFIRMED.getDescription()));

    }
}