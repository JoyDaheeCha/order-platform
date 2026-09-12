package com.flab.orderplatform.order.infrastructure.persistence;

import com.flab.orderplatform.order.application.exception.OrderNotFoundException;
import com.flab.orderplatform.order.application.port.out.OrderViewQueryRepository;
import com.flab.orderplatform.order.application.port.out.view.OrderView;
import com.flab.orderplatform.order.application.port.out.view.QOrderView_OrderItemDto;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import static com.flab.orderplatform.order.domain.QOrder.order;
import static com.flab.orderplatform.order.domain.QOrderItem.orderItem;

@Repository
@RequiredArgsConstructor
public class OrderViewQueryRepositoryImpl implements OrderViewQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public OrderView findByOrderNumber(String orderNumber) {

        var searchedOrder = queryFactory.selectFrom(order)
                .where(order.orderNumber.eq(orderNumber))
                .fetchOne();

        if (searchedOrder == null) {
            throw new OrderNotFoundException(orderNumber);
        }

        var searchedOrderItems = queryFactory.select(
                        new QOrderView_OrderItemDto(
                                orderItem.name,
                                orderItem.quantity,
                                orderItem.price
                        ))
                .from(orderItem)
                .where(orderItem.order.eq(searchedOrder))
                .fetch();

        return new OrderView(searchedOrder.getOrderNumber(), searchedOrder.getStatus(), searchedOrderItems);
    }
}
