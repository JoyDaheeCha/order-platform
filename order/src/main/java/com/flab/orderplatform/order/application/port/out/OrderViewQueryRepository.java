package com.flab.orderplatform.order.application.port.out;

import com.flab.orderplatform.order.application.port.out.view.OrderView;

public interface OrderViewQueryRepository {
    OrderView findByOrderNumber(String orderNumber);
}
