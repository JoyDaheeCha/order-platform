-- 02-orders.sql — order 스키마의 orders 테이블 (스캐폴드).

USE order_schema;

CREATE TABLE IF NOT EXISTS orders (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    order_number VARCHAR(36)  NOT NULL COMMENT '주문번호 (대외 노출용 비즈니스 키)',
    total_amount BIGINT       NOT NULL COMMENT '총 구매 금액',
    ordered_at   DATETIME(6)  NOT NULL COMMENT '주문일자',
    status       VARCHAR(20)  NOT NULL COMMENT '주문 상태 (PENDING/PAID/CONFIRMED/CANCELLED)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_number (order_number)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
