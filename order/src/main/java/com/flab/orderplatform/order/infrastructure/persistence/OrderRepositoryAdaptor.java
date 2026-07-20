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
    public Long save(Order order) {
        var savedEntity = orderJpaRepository.save(OrderEntity.from(order)); // TODO Entity와 VO를 분리하는게 오히려 개발효율성을 떨어트리지 않을까? 지금 어떤 장점이 있지?
        return savedEntity.getId();
    }
}
