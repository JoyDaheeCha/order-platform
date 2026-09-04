-- 재고
CREATE TABLE IF NOT EXISTS inventory
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    product_code        VARCHAR(36)  NOT NULL COMMENT '상품코드',
    stock               INT          NOT NULL COMMENT '가용 재고 수량',
    reserved_stock      INT          NOT NULL COMMENT '선점된 재고 수량',
    created_at          DATETIME(6)  NOT NULL COMMENT '생성일',
    updated_at          DATETIME(6)  NOT NULL COMMENT '수정일',
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_product_code (product_code)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;

-- 재고 변경 히스토리
CREATE TABLE IF NOT EXISTS inventory_history
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    inventory_id        BIGINT       NOT NULL COMMENT '재고 pk',
    order_number        VARCHAR(36)  NOT NULL COMMENT '주문번호 (대외 노출용 비즈니스 키)',
    quantity            INT          NOT NULL COMMENT '변경 수량',
    created_at          DATETIME(6)  NOT NULL COMMENT '생성일',
    updated_at          DATETIME(6)  NOT NULL COMMENT '수정일',
    PRIMARY KEY (id),
    KEY idx_inventory_history_order_number (order_number)
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
    aggregate_type VARCHAR(30) NOT NULL COMMENT '에그리거트명 (예. inventory)',
    aggregate_id VARCHAR(30) NOT NULL COMMENT '에그리거트 식별자',
    event_type VARCHAR(30) NOT NULL COMMENT '이벤트타입',
    topic VARCHAR(50) NOT NULL COMMENT '이벤트 토픽명',
    payload JSON NOT NULL COMMENT '이벤트 페이로드',
    status VARCHAR(10) NOT NULL COMMENT '이벤트 상태 (CREATED/PUBLISHED/FAILED)',
    occurred_at DATETIME(6) NOT NULL COMMENT '이벤트 발행 일시',
    created_at     DATETIME(6)    NOT NULL COMMENT '생성일',
    updated_at     DATETIME(6)    NOT NULL COMMENT '수정일',
    UNIQUE KEY uk_outbox_event_id (event_id),
    KEY idx_outbox_status_created_at (status, created_at)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci;