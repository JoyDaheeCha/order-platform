package com.flab.orderplatform.order.infrastructure.persistence.externals;

import com.flab.orderplatform.order.domain.external.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByProductCodeIn(List<String> productCodes);
}
