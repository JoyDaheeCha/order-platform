package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.application.port.out.OrderRepository;
import com.flab.orderplatform.order.application.query.ProductQueryService;
import com.flab.orderplatform.order.domain.OrderNumberGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreateFacade {
    private final OrderRepository orderRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final ProductQueryService productQueryService;

    @Transactional
    public Long createOrder(OrderCreateCommand command) {
        var orderNumber = orderNumberGenerator.generate();
        var pricesByProductCode = productQueryService.createPriceByProductCodeMap(command.getProductCodes());
        var order = command.createOrder(orderNumber, pricesByProductCode);
        return orderRepository.save(order);
    }
}
