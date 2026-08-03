package com.flab.orderplatform.order.infrastructure.persistence.externals;

import com.flab.orderplatform.order.application.port.out.ProductRepository;
import com.flab.orderplatform.order.domain.external.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdaptor implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;

    @Override
    public List<Product> findAllByProductCodeIn(List<String> productCodes) {
        return productJpaRepository.findAllByProductCodeIn(productCodes);
    }

    @Override
    public List<Product> findAllByIdIn(List<Long> productIds) {
        return productJpaRepository.findAllById(productIds);
    }
}
