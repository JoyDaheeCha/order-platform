package com.flab.orderplatform.order;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * order 모듈 슬라이스 테스트 전용 부트 진입점.
 * 운영 진입점(bootstrap 의 OrderPlatformApplication)은 이 모듈에서 보이지 않으므로,
 *
 * @WebMvcTest 가 탐색할 @SpringBootConfiguration 을 테스트 소스에만 제공한다.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class OrderTestApplication {
}
