// bootstrap — 유일한 "실행 모듈". @SpringBootApplication 과 main()을 두고 전 컨텍스트를 조립한다. (architecture.md §1, §7)
//   · 다른 모듈은 java-library(plain jar). bootJar 는 여기에만 적용된다.
//   · 도메인/애플리케이션 로직이 실행 책임을 갖지 않도록 격리하는 것이 목적.

plugins {
    // 이 모듈에만 Spring Boot 플러그인 적용 → bootJar/bootRun 태스크 활성화.
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    // 전 컨텍스트를 조립한다(스프링 빈 스캔/와이어링 대상). 각 컨텍스트의 infrastructure 패키지가
    // 컴포넌트 스캔으로 잡히고, 그 어댑터가 쓰는 web/jpa/kafka 는 컨텍스트 모듈이 전이로 제공.
    implementation(project(":order"))
    implementation(project(":payment"))
    implementation(project(":inventory"))

    // 최소 실행 스타터(자동설정·로깅).
    implementation(libs.spring.boot.starter)

    // --- 경계 강제 테스트(A-1~A-6)는 전 모듈을 한 클래스패스에서 보는 bootstrap 에서 수행 ---
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.archunit.junit5)
}
