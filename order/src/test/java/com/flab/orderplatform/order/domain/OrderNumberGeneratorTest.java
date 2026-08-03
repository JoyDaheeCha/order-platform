package com.flab.orderplatform.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

@DisplayName("주문번호 생성기 단위테스트")
class OrderNumberGeneratorTest {

    /** 날짜 8자리 + '-' + Crockford base32 10자리. O/I/L/U 는 알파벳에서 제외되어 있다. */
    private static final String ORDER_NUMBER_PATTERN = "^\\d{8}-[0-9A-HJKMNP-TV-Z]{10}$";
    private static final int BULK_COUNT = 10_000;

    private final OrderNumberGenerator orderNumberGenerator = new OrderNumberGenerator();

    @DisplayName("[성공] 생성된 주문번호는 '날짜8자리-영숫자10자리' 형식이다.")
    @Test
    void generate() {
        // when
        var orderNumber = orderNumberGenerator.generate();

        // then
        assertSoftly(softly -> {
            softly.assertThat(orderNumber).matches(ORDER_NUMBER_PATTERN);
            softly.assertThat(orderNumber).hasSize(19);
        });
    }

    @DisplayName("[성공] 주문번호 앞 8자리는 생성 시점의 날짜다.")
    @Test
    void generateStartsWithToday() {
        // given: LocalDate.of 까지 목킹에 걸리지 않도록 고정 날짜는 목킹 밖에서 만든다.
        var fixedToday = LocalDate.of(2026, 7, 21);

        String orderNumber;
        try (MockedStatic<LocalDate> mocked = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mocked.when(LocalDate::now).thenReturn(fixedToday);
            // when
            orderNumber = orderNumberGenerator.generate();
        }

        // then
        assertThat(orderNumber).startsWith("20260721-");
    }

    @DisplayName("[성공] 주문번호에는 혼동하기 쉬운 문자(O, I, L, U)가 쓰이지 않는다.")
    @Test
    void generateExcludesConfusingCharacters() {
        // when
        var orderNumbers = IntStream.range(0, BULK_COUNT)
                .mapToObj(i -> orderNumberGenerator.generate())
                .toList();

        // then: 고객이 주문번호를 눈으로 읽고 옮겨 적을 때 0/O, 1/I/L 을 혼동하지 않도록 한다.
        assertThat(orderNumbers).allSatisfy(orderNumber ->
                assertThat(orderNumber).doesNotContain("O", "I", "L", "U"));
    }

    @DisplayName("[성공] 주문번호를 1만 번 생성해도 중복이 발생하지 않는다.")
    @Test
    void generateDoesNotCollide() {
        // when
        var generated = new HashSet<String>();
        IntStream.range(0, BULK_COUNT).forEach(i -> generated.add(orderNumberGenerator.generate()));

        // then: 중복이 나면 @Retryable 재시도로 흡수되지만, 애초에 32^10 공간에서 충돌이 나면 안 된다.
        assertThat(generated).hasSize(BULK_COUNT);
    }
}
