package com.flab.orderplatform.order.infrastructure;

import com.flab.orderplatform.order.application.port.out.ProductRepository;
import com.flab.orderplatform.order.domain.Product;
import com.flab.orderplatform.order.infrastructure.persistence.externals.ProductJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ProductRepositoryAdaptor implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;

    @Override
    public List<Product> findAllByProductCode(List<String> productCodes) {
        var productEntities = productJpaRepository.findAllByProductCodeIn(productCodes);
        return productEntities.stream().map(product ->
                        Product.builder()
                                .productId(product.getId())
                                .productCode(product.getProductCode())
                                .price(product.getPrice())
                                .build())
                .toList();
    }
}
