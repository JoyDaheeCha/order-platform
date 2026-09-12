package com.flab.orderplatform.order.infrastructure.web;

import com.flab.orderplatform.order.application.OrderCommandHandler;
import com.flab.orderplatform.order.application.OrderCreateFacade;
import com.flab.orderplatform.order.application.command.OrderCancelCommand;
import com.flab.orderplatform.order.domain.Order;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 커멘드 컨트롤러
 */
@RequiredArgsConstructor
@RestController
public class OrderCommandController {

    private final OrderCreateFacade createFacade;
    private final OrderCommandHandler commandHandler;

    /**
     * 주문 생성
     */
    @PostMapping("/order")
    public Long createOrder(@RequestHeader("Idempotency-key") String idempotentKey,
                            @Valid @RequestBody OrderCreateRequest request) {
        return createFacade.createOrder(request.toCommand(idempotentKey));
    }

    // TODO: rest Docs 추가
    /**
     * 주문 취소
     */
    @PatchMapping("/order/{orderNumber}/cancelled")
    public Order cancelOrder(@PathVariable String orderNumber) {
        return commandHandler.cancelOrder(new OrderCancelCommand(orderNumber));
    }
}
