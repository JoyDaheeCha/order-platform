package com.flab.orderplatform.order.domain;

import com.flab.orderplatform.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

/**
 * 주문 상품 정보
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "order_item")
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false, columnDefinition = "BIGINT NOT NULL COMMENT '상품 ID'")
    private Long productId;

    @Column(length = 36, unique = true, nullable = false,
            columnDefinition = "VARCHAR(36) NOT NULL COMMENT '상품코드 (예. GD10001)'")
    private String productCode;

    @Column(name = "name", nullable = false, columnDefinition = "VARCHAR(100) NOT NULL COMMENT '상품명'")
    private String name;

    @Column(name = "price", nullable = false, columnDefinition = "BIGINT NOT NULL COMMENT '상품 가격'")
    private Long price;

    @Column(name = "quantity", nullable = false, columnDefinition = "INT NOT NULL COMMENT '주문 수량'")
    private Integer quantity;

    @Builder
    public OrderItem(Long productId, String productCode, String name, Long price, Integer quantity) {
        this.productId = productId;
        this.productCode = productCode;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public Long calculateAmount() {
        return price * quantity;
    }

    public OrderItem setOrder(Order order) {
        this.order = order;
        return this;
    }
}
