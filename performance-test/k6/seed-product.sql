-- k6 부하테스트용 상품 시드 데이터 (order 모듈이 주문 생성 시 상품코드를 검증하는 테이블)
-- 사용법:
--   mysql -h 127.0.0.1 -P 13306 -uroot -p<비밀번호> order_schema < seed-product.sql
USE order_schema;

INSERT INTO product (product_code, price)
VALUES
    ('PERF-TEST-0001', 10000),
    ('PERF-TEST-0002', 20000),
    ('PERF-TEST-0003', 30000),
    ('PERF-TEST-0004', 40000),
    ('PERF-TEST-0005', 50000)
ON DUPLICATE KEY UPDATE
    price = VALUES(price);
