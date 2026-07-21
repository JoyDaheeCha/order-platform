package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.application.port.out.ProductRepository;
import com.flab.orderplatform.order.domain.OrderNumberGenerator;
import com.flab.orderplatform.order.domain.Product;
import jakarta.transaction.Transactional;
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

    @Transactional
    public Long createOrder(OrderCreateCommand command) {
        var orderNumber = orderNumberGenerator.generate();
        var pricesByProductCode = createPriceByProductCodeMap(command.getProductCodes());
        return orderCommandHandler.handle(orderNumber, pricesByProductCode, command);
    }

    public Map<String, Long> createPriceByProductCodeMap(List<String> productCodes) {
        var products = productRepository.findAllByProductCode(productCodes);
        return products.stream().collect(Collectors.toMap(Product::productCode, Product::price));
    }
}
