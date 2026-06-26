// order-application — 유스케이스(application service) + 포트(in/out interface) + 트랜잭션 경계.
// 의존: 같은 컨텍스트 domain, shared 만. infrastructure 는 절대 의존 금지(A-3).

dependencies {
    // api: 포트 시그니처에 도메인 타입(Order 등)이 노출되므로, 이 모듈을 쓰는 infrastructure 가
    //      domain 을 전이적으로 볼 수 있게 api 로 공개한다.
    api(project(":order:order-domain"))
    // api: 아웃바운드 포트가 shared 의 통합 이벤트를 발행 인자로 받으므로 함께 공개.
    api(project(":shared"))

    // BOM(platform): spring-tx 의 버전을 Spring Boot 가 관리하도록 가져온다(개별 버전 명시 X).
    implementation(platform(libs.spring.boot.dependencies))
    // 최소 프레임워크: @Transactional 추상만. (스프링 컨텍스트/JPA/웹은 여기 들이지 않는다)
    implementation(libs.spring.tx)
}
