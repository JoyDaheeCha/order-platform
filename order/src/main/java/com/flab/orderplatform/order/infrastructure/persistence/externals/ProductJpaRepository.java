package com.flab.orderplatform.order.infrastructure.persistence.externals;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {
    List<ProductEntity> findAllByProductCodeIn(List<String> productCodes);
}
