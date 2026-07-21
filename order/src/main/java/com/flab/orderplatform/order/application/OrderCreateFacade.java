package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.annotation.OrderTransactional;
import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.application.exception.ProductNotFoundException;
import com.flab.orderplatform.order.application.port.out.ProductRepository;
import com.flab.orderplatform.order.domain.OrderNumberGenerator;
import com.flab.orderplatform.order.domain.external.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
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
        var productsByCode = createProductByProductCodeMap(command.getProductCodes());
        var createdOrder = orderCommandHandler.handle(orderNumber, productsByCode, command);
        return createdOrder.getId();
    }

    public Map<String, Product> createProductByProductCodeMap(List<String> productCodes) {
        var products = productRepository.findAllByProductCodeIn(productCodes);

        var productsByProductCodeMap = products.stream()
                .collect(Collectors.toMap(Product::getProductCode, p -> p));

        // 모든 상품코드가 유효하면 정상
        if (productCodes.size() == productsByProductCodeMap.size()) {
            return productsByProductCodeMap;
        }

        var notFound = new LinkedHashSet<>(productCodes);
        notFound.removeAll(productsByProductCodeMap.keySet());
        throw new ProductNotFoundException(notFound);
    }
}
