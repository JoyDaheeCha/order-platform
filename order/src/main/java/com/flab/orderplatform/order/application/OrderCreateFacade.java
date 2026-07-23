package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.command.OrderCreateCommand;
import com.flab.orderplatform.order.application.exception.ProductNotFoundException;
import com.flab.orderplatform.order.application.exception.SystemException;
import com.flab.orderplatform.order.application.port.out.ProductRepository;
import com.flab.orderplatform.order.domain.OrderNumberGenerator;
import com.flab.orderplatform.order.domain.external.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 주문 생성 use case Facade
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreateFacade {
    private final OrderCommandHandler orderCommandHandler;
    private final OrderNumberGenerator orderNumberGenerator;
    private final ProductRepository productRepository;

    /**
     * 주문 생성한다.
     * 생성된 주문번호 생성시, 중복될 경우 retry (최초 1회,재시도 2회)
     */
    @Retryable(
            retryFor = DuplicateKeyException.class,
            backoff = @Backoff(delay = 0)
    )
    public Long createOrder(OrderCreateCommand command) {
        var orderNumber = orderNumberGenerator.generate();
        var productsByCode = createProductByProductCodeMap(command.getProductCodes());
        var createdOrder = orderCommandHandler.handle(orderNumber, productsByCode, command);
        return createdOrder.getId();
    }

    @SuppressWarnings(value = "unused")
    @Recover
    public Long recover(DuplicateKeyException e, OrderCreateCommand command) {
        log.error("주문 번호 생성에 실패하였습니다.", e);
        throw new SystemException("주문 생성에 실패했습니다. 잠시 후 다시 시도해주세요", e);
    }


    private Map<String, Product> createProductByProductCodeMap(List<String> productCodes) {
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
