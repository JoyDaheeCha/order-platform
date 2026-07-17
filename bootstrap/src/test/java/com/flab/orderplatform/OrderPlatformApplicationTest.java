package com.flab.orderplatform;

import com.flab.orderplatform.persistence.MySqlTestContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 애플리케이션 기동 스모크 테스트.
 */
@DisplayName("애플리케이션 기동 스모크 테스트")
@SpringBootTest
class OrderPlatformApplicationTest {

    @DynamicPropertySource
    static void dataSources(DynamicPropertyRegistry registry) {
        MySqlTestContainer.registerDataSources(registry);
    }

    @Test
    @DisplayName("스프링 컨텍스트가 정상적으로 로드된다")
    void contextLoads() {
    }
}
