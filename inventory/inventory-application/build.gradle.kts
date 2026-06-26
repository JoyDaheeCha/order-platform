// inventory-application — 유스케이스 + 포트 + 트랜잭션 경계. 의존: inventory-domain, shared 만. (A-3)

dependencies {
    api(project(":inventory:inventory-domain"))
    api(project(":shared"))
    // BOM(platform): spring-tx 버전을 Spring Boot 가 관리하도록 가져온다.
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.tx)
}
