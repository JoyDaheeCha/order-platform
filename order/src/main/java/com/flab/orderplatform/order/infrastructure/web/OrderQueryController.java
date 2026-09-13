package com.flab.orderplatform.order.infrastructure.web;

import com.flab.orderplatform.order.application.OrderQueryService;
import com.flab.orderplatform.order.application.port.out.view.OrderView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderQueryController {

    private final OrderQueryService orderQueryService;

    /**
     * 주문 조회
     */
    @GetMapping("/order/{orderNumber}")
    public OrderView searchOrder(@PathVariable String orderNumber) {
        return orderQueryService.searchOrder(orderNumber);
    }
}
