package com.flab.orderplatform.order.infrastructure.persistence;

import com.flab.orderplatform.order.application.port.out.OrderRepository;
import com.flab.orderplatform.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdaptor implements OrderRepository {
    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<Order> findWithOrderItemsByOrderNumber(String orderNumber) {
        return orderJpaRepository.findWithOrderItemsByOrderNumber(orderNumber);
    }
}
