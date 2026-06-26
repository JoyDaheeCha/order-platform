// order-infrastructure — 어댑터 계층: JPA 영속화 · Kafka 컨슈머/프로듀서 · REST 컨트롤러 · Outbox/Inbox.
// 의존: 같은 컨텍스트 application(+전이적 domain·shared). 다른 컨텍스트(payment/inventory)는 금지(A-4).

dependencies {
    // implementation: application 을 통해 domain·shared 가 전이적으로 따라온다(application 이 api 로 공개).
    //   포트의 구현체(어댑터)를 여기 두므로 application 에 대한 의존이 필수.
    implementation(project(":order:order-application"))

    // Spring Boot BOM(platform): 아래 starter 들의 버전을 BOM 이 일괄 관리(개별 버전 명시 X).
    implementation(platform(libs.spring.boot.dependencies))

    // 인바운드 어댑터(REST) / 아웃바운드 어댑터(JPA, Kafka).
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.kafka)
    runtimeOnly(libs.mysql.connector)
}
