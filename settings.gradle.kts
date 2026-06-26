// settings.gradle.kts — 멀티모듈 프로젝트의 "구성표".
// 어떤 디렉토리가 Gradle 모듈(subproject)인지 여기서 선언한다. (architecture.md §7)

plugins {
    // Toolchain 자동 프로비저닝: 로컬에 JDK 21이 없어도 Gradle이 직접 내려받아 컴파일에 사용한다.
    // (문서가 Java 21을 명시하므로 로컬 JDK 버전과 무관하게 21로 고정하기 위함)
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "order-platform"

// --- 바운디드 컨텍스트별 3모듈(헥사고날) + 공통 + 실행 모듈 ---
// 주의: order / payment / inventory 디렉토리 자체는 "모듈"이 아니라 하위 모듈을 담는 컨테이너다.
//       (:order:order-domain 처럼 leaf만 include 하면 :order 는 빈 컨테이너 프로젝트로 자동 생성됨)

include(
    ":shared",

    ":order:order-domain",
    ":order:order-application",
    ":order:order-infrastructure",

    ":payment:payment-domain",
    ":payment:payment-application",
    ":payment:payment-infrastructure",

    ":inventory:inventory-domain",
    ":inventory:inventory-application",
    ":inventory:inventory-infrastructure",

    ":bootstrap",
)
