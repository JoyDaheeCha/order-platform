package com.flab.orderplatform.order.application.port.out;

import com.flab.orderplatform.order.domain.Product;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository {
    List<Product> findAllById(List<Long> productIds);
}
