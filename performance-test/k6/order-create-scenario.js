import http from 'k6/http';
import {check, sleep} from 'k6';
import {Counter} from 'k6/metrics';
import {htmlReport} from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';
import {textSummary} from 'https://jslib.k6.io/k6-summary/0.0.3/index.js';

// 사용법:
//   k6 run performance-test/k6/order-create-scenario.js
//   BASE_URL=http://localhost:8080 k6 run performance-test/k6/order-create-scenario.js
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// seed-inventory.sql 로 미리 넣어둔 상품코드. 실제 운영 상품코드로 바꿔도 됨.
const PRODUCT_CODES = (__ENV.PRODUCT_CODES || 'PERF-TEST-0001,PERF-TEST-0002,PERF-TEST-0003,PERF-TEST-0004,PERF-TEST-0005').split(',');

const orderFailures = new Counter('order_failures');

export const options = {
    scenarios: {
        // 1) 스모크: 스크립트/엔드포인트가 정상 동작하는지 최소 부하로 먼저 확인
        smoke: {
            executor: 'constant-vus',
            vus: 1,
            duration: '10s',
            tags: { scenario: 'smoke' },
        },
        // 2) 평균 부하: 스모크 뒤 30초 대기 후 시작, 점진적으로 VU를 올렸다 내림
        average_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 20 },
                { duration: '1m', target: 20 },
                { duration: '30s', target: 0 },
            ],
            startTime: '40s',
            tags: { scenario: 'average_load' },
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
        order_failures: ['count<10'],
    },
};

function randomProductCode() {
    return PRODUCT_CODES[Math.floor(Math.random() * PRODUCT_CODES.length)];
}

// 요청마다 유니크해야 하는 Idempotency-key. VU/이터레이션/시간 조합으로 충돌 없이 생성.
function idempotencyKey() {
    return `perf-${__VU}-${__ITER}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export default function () {
    const payload = JSON.stringify({
        customerId: Math.floor(Math.random() * 100000) + 1,
        orderItemDtos: [
            {
                quantity: Math.floor(Math.random() * 3) + 1,
                name: 'k6 부하테스트 상품',
                productCode: randomProductCode(),
            },
        ],
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Idempotency-key': idempotencyKey(),
        },
    };

    const res = http.post(`${BASE_URL}/order`, payload, params);

    const ok = check(res, {
        'status is 200/201': (r) => r.status === 200 || r.status === 201,
    });
    if (!ok) {
        orderFailures.add(1);
    }

    sleep(1);
}

export function handleSummary(data) {
    return {
        'performance-test/k6/results/summary.html': htmlReport(data),
        'performance-test/k6/results/summary.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
