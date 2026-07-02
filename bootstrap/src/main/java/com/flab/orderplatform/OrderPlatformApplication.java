package com.flab.orderplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 모듈러 모노리스의 단일 실행 진입점. (architecture.md §1)
 *
 * <p>이 클래스가 {@code com.flab.orderplatform} 루트에 있으므로, 컴포넌트 스캔/엔티티 스캔이
 * 전 컨텍스트({@code .order}, {@code .payment}, {@code .inventory})의 빈을 한 번에 조립한다.
 */
@SpringBootApplication
public class OrderPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderPlatformApplication.class, args);
    }
}
