package com.flab.orderplatform.order.infrastructure.persistence.externals;

import com.flab.orderplatform.order.application.port.out.ProductRepository;
import com.flab.orderplatform.order.domain.external.Product;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ProductRepositoryAdaptor implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;

    @Override
    public List<Product> findAllByProductCodeIn(List<String> productCodes) {
        return productJpaRepository.findAllByProductCodeIn(productCodes);
    }
}
