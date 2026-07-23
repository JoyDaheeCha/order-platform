package com.flab.orderplatform.order.infrastructure.persistence;

import com.flab.orderplatform.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

}
