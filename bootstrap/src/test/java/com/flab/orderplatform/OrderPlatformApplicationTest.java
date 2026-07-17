package com.flab.orderplatform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 본 테스트 실행시 docker compose up -d 으로 redis,mysql,kafka 가 먼저 띄워져있어야한다.
 */
@DisplayName("애플리케이션 기동 스모크 테스트")
@SpringBootTest
class OrderPlatformApplicationTest {

    @Test
    @DisplayName("스프링 컨텍스트가 정상적으로 로드된다")
    void contextLoads() {
    }
}
