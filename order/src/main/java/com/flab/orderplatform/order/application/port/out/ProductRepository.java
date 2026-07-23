package com.flab.orderplatform.order.application.port.out;

import com.flab.orderplatform.order.domain.external.Product;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository {
    List<Product> findAllByProductCodeIn(List<String> productCodes);
}
