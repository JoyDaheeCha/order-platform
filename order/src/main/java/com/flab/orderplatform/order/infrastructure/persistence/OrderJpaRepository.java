package com.flab.orderplatform.order.infrastructure.persistence;

import com.flab.orderplatform.order.domain.Order;
import com.flab.orderplatform.order.domain.status.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"orderItems"})
    Optional<Order> findWithOrderItemsByOrderNumber(String orderNumber);

    // TODO: 쿼리 똑바로 나가는지 보기
    @Query("""
            select o from Order o
            WHERE o.inventoryReservation.isReleased = false
                        and o.status = :orderStatus
            and o.inventoryReservation.reservedAt < :reservedAtThreshold
            """)
    List<Order> findReleaseTarget(@Param("reservedAtThreshold") LocalDateTime reservedAtThreshold, @Param("orderStatus") OrderStatus orderStatus);

    Optional<Order> findByOrderNumber(String orderNumber);
}
