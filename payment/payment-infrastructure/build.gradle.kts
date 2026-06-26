// payment-infrastructure — 어댑터: JPA · Kafka · REST · Outbox/Inbox. 의존: payment-application 만(+전이). (A-4)

dependencies {
    implementation(project(":payment:payment-application"))

    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.kafka)
    runtimeOnly(libs.mysql.connector)
}
