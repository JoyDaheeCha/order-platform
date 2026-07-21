package com.flab.orderplatform.order.infrastructure.persistence;

import com.flab.orderplatform.order.application.port.out.OrderRepository;
import com.flab.orderplatform.order.domain.Order;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class OrderRepositoryAdaptor implements OrderRepository {
    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }
}
