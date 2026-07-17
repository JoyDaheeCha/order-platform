package com.flab.orderplatform;

import com.flab.orderplatform.persistence.MySqlTestContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 애플리케이션 기동 스모크 테스트.
 *
 * <p>Day 2 부터 {@code docker compose up -d} 를 요구하지 않는다 — 테스트가 자기 MySQL 을 직접
 * 띄운다(Testcontainers). 외부 컨테이너에 의존하면 "누가 언제 띄웠느냐"에 결과가 갈리기 때문이다.
 * 실제로 {@code 02-ping.sql} 을 추가하자 <b>이미 떠 있던</b> 컨테이너엔 그 테이블이 없어 이 테스트가
 * 깨졌다 — MySQL init 스크립트는 데이터 디렉토리가 빈 <b>최초 부팅에만</b> 실행되기 때문이다.
 * (로컬 {@code bootRun} 은 여전히 compose 를 쓰므로, init 스크립트를 바꾸면
 * {@code docker compose down -v} 로 볼륨을 지우고 다시 띄워야 반영된다.)
 *
 * <p>Kafka·Redis 는 아직 붙지 않는다: 커넥션이 지연 생성이라 부팅 시점엔 접속하지 않는다.
 * 실제 연동은 각 어댑터가 생기는 Day 6~8 에 검증한다.
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
