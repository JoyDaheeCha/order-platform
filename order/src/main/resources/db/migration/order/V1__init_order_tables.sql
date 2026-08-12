CREATE TABLE IF NOT EXISTS orders
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    order_number    VARCHAR(36) NOT NULL COMMENT '주문번호 (대외 노출용 비즈니스 키)',
    total_amount    BIGINT      NOT NULL COMMENT '총 구매 금액',
    ordered_at      DATETIME(6) NOT NULL COMMENT '주문일자',
    status          VARCHAR(20) NOT NULL COMMENT '주문 상태 (PENDING/PAID/CONFIRMED/CANCELLED)',
    customer_id     BIGINT      NOT NULL COMMENT '구매자 ID',
    idempotent_key  VARCHAR(36) NOT NULL COMMENT '주문 생성 멱등키',
    created_at      DATETIME(6) NOT NULL COMMENT '생성일',
    updated_at      DATETIME(6) NOT NULL COMMENT '수정일',
    created_by      VARCHAR(20) NOT NULL COMMENT '생성자',
    updated_by      VARCHAR(20) NOT NULL COMMENT '수정자',
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_number (order_number),
    UNIQUE KEY uk_orders_idempotent_key (idempotent_key)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS product
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    product_code VARCHAR(36) NOT NULL COMMENT '상품코드 (예. GD10001)',
    price        BIGINT      NOT NULL COMMENT '상품가격',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_product_code (product_code)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS order_item
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    order_id   BIGINT       NOT NULL COMMENT '주문 ID (orders.id)',
    product_id BIGINT       NOT NULL COMMENT '상품 ID',
    name       VARCHAR(100) NOT NULL COMMENT '상품명',
    price      BIGINT       NOT NULL COMMENT '상품 가격',
    quantity   INT          NOT NULL COMMENT '주문 수량',
    created_at DATETIME(6)  NOT NULL COMMENT '생성일',
    updated_at DATETIME(6)  NOT NULL COMMENT '수정일',
    created_by VARCHAR(20)  NOT NULL COMMENT '생성자',
    updated_by VARCHAR(20)  NOT NULL COMMENT '수정자',
    PRIMARY KEY (id),
    KEY idx_order_item_order_id (order_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;

-- 인박스 패턴 구현용 테이블
CREATE TABLE IF NOT EXISTS inbox
(
    id       BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '이벤트 UUID',
    aggregate_type VARCHAR(30) NOT NULL COMMENT '에그리거트명 (예. payment)',
    event_type VARCHAR(30) NOT NULL COMMENT '이벤트타입',
    payload LONGTEXT NOT NULL COMMENT '이벤트 페이로드', -- 추후 타 도메인에서 메시지 받을때 규격 안 맞더라도 일단 저장은 가능해야하므로 longtext로 저장
    status VARCHAR(10) NOT NULL COMMENT '이벤트 상태 (CREATED/PROCESSED/FAILED)',
    created_at     DATETIME(6)    NOT NULL COMMENT '생성일',
    updated_at     DATETIME(6)    NOT NULL COMMENT '수정일',
    PRIMARY KEY (id),
    KEY idx_inbox_status (status),
    UNIQUE KEY uk_inbox_event_id (event_id)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT '인박스';


-- 아웃박스 패턴용 테이블
CREATE TABLE IF NOT EXISTS outbox
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '이벤트 UUID',
    aggregate_type VARCHAR(30) NOT NULL COMMENT '에그리거트명 (예. order)',
    aggregate_id VARCHAR(30) NOT NULL COMMENT '에그리거트 식별자',
    event_type VARCHAR(30) NOT NULL COMMENT '이벤트타입',
    topic VARCHAR(50) NOT NULL COMMENT '이벤트 토픽명',
    payload JSON NOT NULL COMMENT '이벤트 페이로드',
    status VARCHAR(10) NOT NULL COMMENT '이벤트 상태 (CREATED/PUBLISHED/FAILED)',
    occurred_at DATETIME(6) NOT NULL COMMENT '이벤트 발행 일시',
    created_at DATETIME(6) NOT NULL COMMENT '생성일',
    updated_at DATETIME(6) NOT NULL COMMENT '수정일',
    UNIQUE KEY uk_outbox_event_id (event_id),
    KEY idx_outbox_status_created_at (status, created_at)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;