import http from 'k6/http';
import {check} from 'k6';
import {Counter} from 'k6/metrics';
import {htmlReport} from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';
import {textSummary} from 'https://jslib.k6.io/k6-summary/0.0.3/index.js';

// 처리량 한계(breaking point) 탐색용 계단식 부하테스트.
// 사용법: k6 run performance-test/k6/order-create-stress-scenario.js
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PRODUCT_CODES = (__ENV.PRODUCT_CODES || 'PERF-TEST-0001,PERF-TEST-0002,PERF-TEST-0003,PERF-TEST-0004,PERF-TEST-0005').split(',');

const orderFailures = new Counter('order_failures');

// constant-arrival-rate(개방형 모델)는 sleep 없이 목표 요청률(rate)을 그대로 유지한다.
// ramping-vus처럼 동시 사용자 수로 부하를 주는 게 아니라 "초당 몇 건"을 직접 고정하기 때문에
// 서버가 느려지면 그 지연이 VU 소진(dropped_iterations)이나 지연시간 증가로 바로 드러난다.
const STEP_DURATION_SEC = 30;
const STEPS = [50, 100, 200, 400, 600, 800, 1000]; // 초당 요청 수(req/s) 단계

function buildScenarios() {
    const scenarios = {};
    STEPS.forEach((rate, i) => {
        const name = `rate_${rate}`;
        scenarios[name] = {
            executor: 'constant-arrival-rate',
            rate,
            timeUnit: '1s',
            duration: `${STEP_DURATION_SEC}s`,
            preAllocatedVUs: Math.min(rate, 300),
            maxVUs: Math.min(rate * 3, 1000),
            startTime: `${i * STEP_DURATION_SEC}s`,
            tags: { step: name },
            exec: 'createOrder',
        };
    });
    return scenarios;
}

function buildThresholds() {
    const thresholds = {};
    STEPS.forEach((rate) => {
        const name = `rate_${rate}`;
        // abortOnFail 없이 느슨한 관찰용 threshold. 어느 단계에서 붉게 표시되는지가 곧 한계 지점.
        thresholds[`http_req_duration{step:${name}}`] = ['p(95)<1000'];
        thresholds[`http_req_failed{step:${name}}`] = ['rate<0.05'];
    });
    return thresholds;
}

export const options = {
    scenarios: buildScenarios(),
    thresholds: buildThresholds(),
};

function randomProductCode() {
    return PRODUCT_CODES[Math.floor(Math.random() * PRODUCT_CODES.length)];
}

function idempotencyKey() {
    return `perf-${__VU}-${__ITER}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export function createOrder() {
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
}

export function handleSummary(data) {
    return {
        'performance-test/k6/results/stress-summary.html': htmlReport(data),
        'performance-test/k6/results/stress-summary.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
