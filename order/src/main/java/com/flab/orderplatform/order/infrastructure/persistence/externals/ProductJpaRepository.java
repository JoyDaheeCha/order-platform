package com.flab.orderplatform.order.infrastructure.persistence.externals;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {
}
