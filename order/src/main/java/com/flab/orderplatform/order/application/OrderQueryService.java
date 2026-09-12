package com.flab.orderplatform.order.application;

import com.flab.orderplatform.order.application.port.out.OrderViewQueryRepository;
import com.flab.orderplatform.order.application.port.out.view.OrderView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderQueryService {
    private final OrderViewQueryRepository queryRepository;

    /**
     * 주문 조회
     * @param orderNumber 주문번호
     * @return 주문 및 상품 정보
     */
    public OrderView searchOrder(String orderNumber) {
        return queryRepository.findByOrderNumber(orderNumber);
    }
}
