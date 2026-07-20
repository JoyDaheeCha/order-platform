package com.flab.orderplatform.order.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 본 모듈 내 시간 clock 인스턴스 설정시 시스템 기본 타임존을 따라가도
 */
@Configuration
public class TimeConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
