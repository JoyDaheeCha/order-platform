package com.flab.orderplatform.order.infrastructure.persistence.externals;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 시스템의 상품 복제본.
 * 주문 생성시 가격 정보를 계산하기 위해 사용
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "product")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36, unique = true, nullable = false,
            columnDefinition = "VARCHAR(36) NOT NULL COMMENT '상품코드 (예. GD10001)'")
    private String productCode;

    @Column(name = "price", nullable = false, columnDefinition = "BIGINT NOT NULL COMMENT '상품가격'")
    private Long price;
}
