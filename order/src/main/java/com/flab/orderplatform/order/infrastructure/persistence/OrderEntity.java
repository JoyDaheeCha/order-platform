package com.flab.orderplatform.order.infrastructure.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Order 컨텍스트의 영속화 모델
 *
 * TODO: Day3 작업에서 order 도메인 중심 로직 넣을때, 필요한 필드 추가. (현재는 order, payment, inventory간 경계가 물리적으로 제약되어 있는지 테스트할 용도로 일부 필드만 넣어두었음.)
 */
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @Column(length = 36)
    private String id;

    /** ★ 네이밍 전략 미끼. 기대 컬럼은 {@code total_amount}. 통화는 KRW 정수(policy §0). */
    private long totalAmount;

    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;

    @Column(length = 20)
    private String status;

    /** JPA 요구사항(프록시·리플렉션용 기본 생성자). */
    protected OrderEntity() {
    }

    public OrderEntity(String id, long totalAmount, LocalDateTime orderedAt, String status) {
        this.id = id;
        this.totalAmount = totalAmount;
        this.orderedAt = orderedAt;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public String getStatus() {
        return status;
    }
}
