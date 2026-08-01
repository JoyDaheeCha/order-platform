package com.flab.orderplatform.order.application.port.out;

import com.flab.orderplatform.order.domain.external.Product;

import java.util.List;

public interface ProductRepository {
    List<Product> findAllByProductCodeIn(List<String> productCodes);
}
