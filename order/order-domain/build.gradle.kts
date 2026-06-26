// order-domain — 순수 도메인. Aggregate · VO · 도메인 이벤트 · 불변식 · 도메인 서비스.
// 의존성 0 (순수 Java). Spring/JPA/Kafka/shared 어느 것도 의존하지 않는다. (architecture.md §2, A-1·A-2)
//   · shared 조차 의존 안 함: shared 의 "통합 이벤트"와 도메인의 "도메인 이벤트"는 다른 개념(C-4).
//   · 이 순수성이 ArchUnit A-1 로 강제된다.

// 추가 의존성 없음 — 의존이 생기는 순간 도메인이 오염된다.
