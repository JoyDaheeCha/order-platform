package com.flab.orderplatform.order.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 상품 정보
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "order_item")
public class OrderItemEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: 상품코드로 바꾸는게 나을지 검토 (인벤토리까지 연동될 부분 생각하면, 인벤토리에서는 결국 상품 코드를 바라보기 때문에, 상품 코드가 낫다. 아니면 이벤트 발행할때마다 join 해야함)
    @Column(name = "product_id", nullable = false, columnDefinition = "BIGINT NOT NULL COMMENT '상품 ID'")
    private Long productId;

    @Column(name = "name", nullable = false, columnDefinition = "VARCHAR(100) NOT NULL COMMENT '상품명")
    private String name;

    @Column(name = "price", nullable = false, columnDefinition = "BIGINT NOT NULL COMMENT '상품 가격'")
    private Long price;

    @Column(name = "quantity", nullable = false, columnDefinition = "INT NOT NULL COMMENT '주문 수량'")
    private Integer quantity;

    @Builder
    public OrderItemEntity(Long productId, String name, Long price, Integer quantity) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }
}
