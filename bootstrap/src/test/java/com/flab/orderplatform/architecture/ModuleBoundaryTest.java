package com.flab.orderplatform.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * 모듈 경계 규칙을 "코드로" 강제한다.
 *
 * <p>코드 리뷰가 아니라 테스트가 경계를 지킨다 — 잘못된 의존이 들어오면 빌드가 깨진다.
 */
@DisplayName("모듈 경계 규칙 (ArchUnit)")
class ModuleBoundaryTest {

    private static final String ROOT = "com.flab.orderplatform";
    private static final String SHARED = ROOT + ".shared..";
    /**
     * 도메인이 유일하게 알아도 되는 shared 하위 패키지 = 통합 이벤트 계약(C-4).
     */
    private static final String SHARED_EVENT = ROOT + ".shared.event..";
    /**
     * 공유 커널: 모든 컨텍스트가 상속으로 재사용하는 도메인 기반 클래스(BaseEntity/DomainEvent).
     * */
    private static final String SHARED_DOMAIN = ROOT + ".shared.domain..";
    /**
     * 도메인이 의존해도 되는 shared 화이트리스트 — 이 둘 외의 shared 는 인프라로 간주한다.
     */
    private static final String[] DOMAIN_ALLOWED_SHARED = { SHARED_EVENT, SHARED_DOMAIN };

    /**
     * 도메인 모델을 JPA 엔티티 겸용으로 두기로 했으므로
     * 도메인이 알아도 되는 Spring 은 <b>영속화 매핑·감사 애노테이션</b>뿐이다.
     */
    private static final String[] DOMAIN_ALLOWED_SPRING = {
            "org.springframework.data.annotation..",
            "org.springframework.data.jpa.domain.support..",
    };

    /**
     * 영속화와 무관한 인프라 — 도메인은 "어떻게 전송·호출되는지"를 알면 안 된다.
     */
    private static final String[] DOMAIN_FORBIDDEN_INFRA = {
            "org.apache.kafka..",
            "org.redisson..",
            "com.fasterxml.jackson..",
    };

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        // 우리 프로덕션 클래스만 임포트(테스트 클래스 제외). 외부 라이브러리(org.springframework 등)는
        // 패키지가 달라 자동으로 분석 대상에서 빠지지만, 의존 "대상"으로는 규칙에서 참조한다.
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    /**
     * {@code com.flab.orderplatform.<context>..} 형태의 완전한 패키지 패턴을 만든다.
     */
    private static String[] contextPackages(String... contexts) {
        String[] packages = new String[contexts.length];
        for (int i = 0; i < contexts.length; i++) {
            packages[i] = "%s.%s..".formatted(ROOT, contexts[i]);
        }
        return packages;
    }

    /**
     * 컨텍스트는 다른 컨텍스트 패키지를 의존하지 않는다.
     */
    private static void assertNoCrossContextDependency(String self, String... others) {
        String[] otherPackages = new String[others.length];
        for (int i = 0; i < others.length; i++) {
            otherPackages[i] = "..%s..".formatted(others[i]);
        }
        noClasses()
                .that().resideInAPackage("..%s..".formatted(self))
                .should().dependOnClassesThat().resideInAnyPackage(otherPackages)
                .because("컨텍스트 간 통신은 shared 통합 이벤트(Kafka)로만 한다")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("domain 은 영속화 매핑까지만 허용한다 (Spring DI·웹·Kafka·Jackson 금지)")
    void domainShouldOnlyKnowPersistenceMapping() {
        DescribedPredicate<JavaClass> forbidden =
                resideInAPackage("org.springframework..")
                        .and(not(resideInAnyPackage(DOMAIN_ALLOWED_SPRING)))
                        .or(resideInAnyPackage(DOMAIN_FORBIDDEN_INFRA))
                        .as("영속화 매핑·감사 애노테이션이 아닌 프레임워크 클래스");

        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat(forbidden)
                .because("도메인은 'DB에 어떻게 저장되는지'까지만 알고, '누가 호출하고 어떻게 전송되는지'는 모른다")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("domain 은 같은 컨텍스트의 application·infrastructure 를 의존하지 않는다")
    void domainShouldNotDependOnOuterLayers() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..application..", "..infrastructure..")
                .because("의존은 안쪽(domain)으로만 향한다")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("application 은 infrastructure 를 의존하지 않는다")
    void applicationShouldNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because("포트는 application 에 있고, 구현(어댑터)은 infrastructure 가 의존을 역전한다")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("레이어 의존 방향 infrastructure → application → domain (역방향 금지)")
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
                .because("안쪽 레이어는 바깥을 모른다")
                .check(classes);
    }

    @Test
    @DisplayName("바운디드 컨텍스트 간 컴파일 의존 금지 (order ↔ payment ↔ inventory)")
    void contextsShouldNotDependOnEachOther() {
        assertNoCrossContextDependency("order", "payment", "inventory");
        assertNoCrossContextDependency("payment", "order", "inventory");
        assertNoCrossContextDependency("inventory", "order", "payment");
    }

    @Test
    @DisplayName("domain 이 의존할 수 있는 shared 는 통합 이벤트 계약·공유 커널뿐이다")
    void domainMayOnlyDependOnSharedEventContract() {
        noClasses()
                .that().resideInAnyPackage(contextPackages("order", "payment", "inventory"))
                .and().resideInAPackage("..domain..")
                .should().dependOnClassesThat(
                        resideInAPackage(SHARED)
                                .and(not(resideInAnyPackage(DOMAIN_ALLOWED_SHARED)))
                                .as("통합 이벤트 계약·공유 커널이 아닌 shared 클래스"))
                .because("도메인은 통합 이벤트 계약과 공유 커널만 안다. 그 외 shared(아웃박스·인박스·메시징)가 도메인으로 새면 안 된다")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("shared 는 어떤 바운디드 컨텍스트도 의존하지 않는다 (역방향 의존 금지)")
    void sharedShouldNotDependOnAnyContext() {
        noClasses()
                .that().resideInAPackage(SHARED)
                .should().dependOnClassesThat()
                .resideInAnyPackage(contextPackages("order", "payment", "inventory"))
                .because("shared 는 계약만 담는다. 컨텍스트를 알기 시작하면 '분산된 모놀리스'가 된다")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("shared의 이벤트는 프레임워크를 의존하지 않는다")
    void sharedShouldBeFrameworkFree() {
        noClasses()
                .that().resideInAPackage("..shared.event..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.apache.kafka..",
                        "com.fasterxml.jackson..")
                .because("shared 가 프레임워크를 알면 계약 라이브러리(jar)로 떼어낼 수 없다. 직렬화는 각 컨텍스트 infrastructure 책임(C-4)")
                .allowEmptyShould(true)
                .check(classes);
    }
}
