/**
 * Order 순수 도메인: Aggregate·VO·도메인 이벤트·불변식. 프레임워크 의존 0.
 *
 * <p>별도 모듈이 아니라 패키지다 — 도메인 순수성(A-1: Spring/JPA/Kafka 의존 금지)은
 * 컴파일이 아니라 ArchUnit(ModuleBoundaryTest)이 강제한다.
 */
package com.flab.orderplatform.order.domain;
