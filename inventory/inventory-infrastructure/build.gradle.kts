// inventory-infrastructure — 어댑터: JPA · Kafka · REST · Outbox/Inbox + 재고 동시성 제어. 의존: inventory-application 만(+전이). (A-4)

dependencies {
    implementation(project(":inventory:inventory-application"))

    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.kafka)
    runtimeOnly(libs.mysql.connector)

    // 재고 차감 동시성 제어(분산 락 등) 후보 — ADR-0002 에서 기법 확정 예정.
    implementation(libs.spring.boot.starter.data.redis)
}
