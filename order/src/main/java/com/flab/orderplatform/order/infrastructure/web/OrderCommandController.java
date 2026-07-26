package com.flab.orderplatform.order.infrastructure.web;

import com.flab.orderplatform.order.application.OrderCreateFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class OrderCommandController {

    private final OrderCreateFacade orderService;

    @PostMapping("/order")
    public Long createOrder(@RequestHeader("Idempotency-key") String idempotentKey,
                            @Valid @RequestBody OrderCreateRequest request) {
        return orderService.createOrder(request.toCommand(idempotentKey));
    }
}
