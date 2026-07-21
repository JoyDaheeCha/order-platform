package com.flab.orderplatform.order.infrastructure.web;

import com.flab.orderplatform.order.application.OrderCreateFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

// TODO controller E2E 테스트 추가
// TODO 봉투패턴 추가
@RequiredArgsConstructor
@RestController
public class OrderCommandController {

    private final OrderCreateFacade orderService;

    // TODO: 사용자가 post 요청을 두번 보냈을 때 1번만 처리되도록 서버에서 방어로직 추가 (방법: Idempotency 사용)
    @PostMapping("/order")
    public Long createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return orderService.createOrder(request.toCommand());
    }
}
