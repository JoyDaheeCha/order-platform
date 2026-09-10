package com.flab.orderplatform.order.infrastructure.persistence;

import com.flab.orderplatform.order.application.port.out.OrderRepository;
import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.status.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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

    @Override
    public List<Order> findReleaseTarget(LocalDateTime reservedAtThreshold, OrderStatus orderStatus) {
        return orderJpaRepository.findReleaseTarget(reservedAtThreshold, orderStatus);
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return orderJpaRepository.findByOrderNumber(orderNumber);
    }
}
