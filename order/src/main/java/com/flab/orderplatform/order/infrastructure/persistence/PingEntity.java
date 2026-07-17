package com.flab.orderplatform.order.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * <b>배관 점검용 더미</b> — 도메인이 아니다. Day 4 에서 {@code OrderEntity} 가 오면 삭제한다(todo.md).
 *
 * <p><b>왜 진짜 OrderEntity 를 안 쓰나</b>: Order Aggregate 는 Day 3 에 설계된다. 영속화 모델을
 * 도메인 모델보다 먼저 쓰면 Day 3 의 도메인이 이미 있는 테이블에 끌려간다 — DDD 프로젝트가
 * 싸우는 DB-first 설계다. 이 클래스가 <b>의도적으로 무의미한 덕에</b> 도메인에 어떤 의견도
 * 표명하지 않으면서 배관만 증명한다. 트랜잭션 매니저는 엔티티가 뭔지 신경 쓰지 않는다.
 *
 * <p><b>{@code noteText} 가 camelCase 인 이유</b>: 네이밍 전략(CamelCaseToUnderscoresNamingStrategy)이
 * 빠지면 이 필드가 {@code note_text} 로 안 바뀌어 {@code validate} 가 부팅을 깬다. 함정을
 * <b>더미 테이블 하나 놓고 오늘</b> 터뜨리기 위한 미끼다. (@Column 을 안 붙인 것도 같은 이유 —
 * 이름을 명시하면 전략이 빠져도 통과해 버린다.)
 */
@Entity
@Table(name = "ping")
public class PingEntity {

    @Id
    @Column(length = 36)
    private String id;

    /** ★ 네이밍 전략 미끼. 기대 컬럼은 {@code note_text}. */
    private String noteText;

    /** JPA 요구사항(프록시·리플렉션용 기본 생성자). */
    protected PingEntity() {
    }

    public PingEntity(String id, String noteText) {
        this.id = id;
        this.noteText = noteText;
    }

    public String getId() {
        return id;
    }

    public String getNoteText() {
        return noteText;
    }
}
