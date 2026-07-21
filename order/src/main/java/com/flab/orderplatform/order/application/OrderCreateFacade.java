package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.annotation.OrderTransactional;
import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.application.port.out.ProductRepository;
import com.flab.orderplatform.order.domain.OrderNumberGenerator;
import com.flab.orderplatform.order.domain.external.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 주문 생성 use case Facade
 */
@Component
@RequiredArgsConstructor
public class OrderCreateFacade {
    private final OrderCommandHandler orderCommandHandler;
    private final OrderNumberGenerator orderNumberGenerator;
    private final ProductRepository productRepository;

    @OrderTransactional
    public Long createOrder(OrderCreateCommand command) {
        var orderNumber = orderNumberGenerator.generate();
        var pricesByProductCode = createPriceByProductCodeMap(command.getProductCodes());
        var createdOrder = orderCommandHandler.handle(orderNumber, pricesByProductCode, command);
        return createdOrder.getId();
    }

    public Map<String, Product> createPriceByProductCodeMap(List<String> productCodes) {
        var products = productRepository.findAllByProductCodeIn(productCodes);
        return products.stream().collect(Collectors.toMap(Product::getProductCode, p -> p));
    }
}
