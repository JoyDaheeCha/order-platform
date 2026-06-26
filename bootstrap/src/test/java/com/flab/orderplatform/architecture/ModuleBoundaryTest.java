package com.flab.orderplatform.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * 모듈 경계 규칙(architecture.md §6, A-1~A-6)을 "코드로" 강제한다.
 *
 * <p>코드 리뷰가 아니라 테스트가 경계를 지킨다 — 잘못된 의존이 들어오면 빌드가 깨진다.
 * bootstrap 은 전 모듈을 한 클래스패스에서 보므로 이 검증의 적임지다. (architecture.md §6)
 */
@DisplayName("모듈 경계 규칙 (ArchUnit)")
class ModuleBoundaryTest {

    private static final String ROOT = "com.flab.orderplatform";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        // 우리 프로덕션 클래스만 임포트(테스트 클래스 제외). 외부 라이브러리(org.springframework 등)는
        // 패키지가 달라 자동으로 분석 대상에서 빠지지만, 의존 "대상"으로는 규칙에서 참조한다.
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @Test
    @DisplayName("A-1: domain 은 Spring·JPA·Kafka 를 의존하지 않는다 (순수성)")
    void domainShouldBeFrameworkFree() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.apache.kafka..",
                        "org.springframework.kafka..")
                .because("A-1: 도메인은 기술로부터 격리된 순수 Java 여야 한다")
                // 스캐폴드 단계엔 도메인 클래스가 아직 없다 → 0개면 통과, 클래스가 생기면 자동 강제.
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("A-2: domain 은 같은 컨텍스트의 application·infrastructure 를 의존하지 않는다")
    void domainShouldNotDependOnOuterLayers() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..application..", "..infrastructure..")
                .because("A-2: 의존은 안쪽(domain)으로만 향한다")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("A-3: application 은 infrastructure 를 의존하지 않는다")
    void applicationShouldNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because("A-3: 포트는 application 에 있고, 구현(어댑터)은 infrastructure 가 의존을 역전한다")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("A-6: 레이어 의존 방향 infrastructure → application → domain (역방향 금지)")
    void layerDependenciesShouldPointInward() {
        layeredArchitecture().consideringOnlyDependenciesInLayers()
                // 스캐폴드 단계엔 각 레이어가 비어 있을 수 있다 → 빈 레이어 허용(코드 생기면 강제).
                .withOptionalLayers(true)
                .layer("Domain").definedBy("..domain..")
                .layer("Application").definedBy("..application..")
                .layer("Infrastructure").definedBy("..infrastructure..")
                // domain 은 누구에게도 의존받기만 하고, 위 레이어를 알지 못한다.
                .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
                .because("A-6: 안쪽 레이어는 바깥을 모른다")
                .check(classes);
    }

    @Test
    @DisplayName("A-4: 바운디드 컨텍스트 간 컴파일 의존 금지 (order ↔ payment ↔ inventory)")
    void contextsShouldNotDependOnEachOther() {
        assertNoCrossContextDependency("order", "payment", "inventory");
        assertNoCrossContextDependency("payment", "order", "inventory");
        assertNoCrossContextDependency("inventory", "order", "payment");
    }

    /**
     * A-4 + A-5: {@code self} 컨텍스트는 다른 컨텍스트 패키지를 의존 MUST NOT.
     * 컨텍스트 간 유일한 공유 통로는 {@code ..shared..}(통합 이벤트)뿐이다(A-5).
     */
    private static void assertNoCrossContextDependency(String self, String... others) {
        String[] otherPackages = new String[others.length];
        for (int i = 0; i < others.length; i++) {
            otherPackages[i] = "..%s..".formatted(others[i]);
        }
        noClasses()
                .that().resideInAPackage("..%s..".formatted(self))
                .should().dependOnClassesThat().resideInAnyPackage(otherPackages)
                .because("A-4/A-5: 컨텍스트 간 통신은 shared 통합 이벤트(Kafka)로만 한다")
                .allowEmptyShould(true)
                .check(classes);
    }
}
