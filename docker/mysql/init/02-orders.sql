-- 02-orders.sql — order 스키마의 orders 테이블 (스캐폴드).

USE order_schema;

CREATE TABLE IF NOT EXISTS orders (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    order_number VARCHAR(36)  NOT NULL COMMENT '주문번호 (대외 노출용 비즈니스 키)',
    total_amount BIGINT       NOT NULL COMMENT '총 구매 금액',
    ordered_at   DATETIME(6)  NOT NULL COMMENT '주문일자',
    status       VARCHAR(20)  NOT NULL COMMENT '주문 상태 (PENDING/PAID/CONFIRMED/CANCELLED)',
    customer_id BIGINT NOT NULL COMMENT '구매자 ID',
    created_at DATETIME
(
    6
) NOT NULL COMMENT '생성일',
    updated_at DATETIME
(
    6
) NOT NULL COMMENT '수정일',
    created_by VARCHAR
(
    20
) NOT NULL COMMENT '생성자',
    updated_by VARCHAR
(
    20
) NOT NULL COMMENT '수정자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_number (order_number)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS product
(
    id
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    product_code
    VARCHAR
(
    36
) NOT NULL COMMENT '상품코드 (예. GD10001)',
    price BIGINT NOT NULL COMMENT '상품가격',
    PRIMARY KEY
(
    id
),
    UNIQUE KEY uk_product_product_code
(
    product_code
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS order_item
(
    id
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    order_id
    BIGINT
    NOT
    NULL
    COMMENT
    '주문 ID (orders.id)',
    product_id
    BIGINT
    NOT
    NULL
    COMMENT
    '상품 ID',
    name
    VARCHAR
(
    100
) NOT NULL COMMENT '상품명',
    price BIGINT NOT NULL COMMENT '상품 가격',
    quantity INT NOT NULL COMMENT '주문 수량',
    created_at DATETIME
(
    6
) NOT NULL COMMENT '생성일',
    updated_at DATETIME
(
    6
) NOT NULL COMMENT '수정일',
    created_by VARCHAR
(
    20
) NOT NULL COMMENT '생성자',
    updated_by VARCHAR
(
    20
) NOT NULL COMMENT '수정자',
    PRIMARY KEY
(
    id
),
    KEY idx_order_item_order_id
(
    order_id
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;
