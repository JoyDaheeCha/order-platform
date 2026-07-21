package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.application.port.out.OrderRepository;
import com.flab.orderplatform.order.application.port.out.ProductRepository;
import com.flab.orderplatform.order.domain.OrderNumberGenerator;
import com.flab.orderplatform.order.domain.Product;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {  // TODO OrderCreateFacade로 분리해야하지 않을까?
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderNumberGenerator orderNumberGenerator;

    @Transactional
    public Long createOrder(OrderCreateCommand command) {
        var orderNumber = orderNumberGenerator.generate();
        var products = productRepository.findAllByProductCode(command.getProductCodes()); // TODO PRODUCT SERVICE 로 분리해야하지 않을까?
        var productsPricesByProductCode = products.stream().collect(Collectors.toMap(Product::productCode, Product::price));
        var order = command.createOrder(orderNumber, productsPricesByProductCode);
        return orderRepository.save(order);
    }
}
