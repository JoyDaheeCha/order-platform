package com.flab.orderplatform.order.domain;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * <p>
 * 주문번호 생성기
 *
 * 숫자 10개 + 알파벳 22개 로 19개 자리의 주문번호를 만듭니다. (날짜 + Crockford base32 10자리)
 *
 * 0/O, 1/I/L 같은 혼용문자는 사용하지 않습니다.
 * 예. 20260720-7K3M9QX2WF
 * </p>
 */
public class OrderNumberGenerator {
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int RANDOM_LENGTH = 10;
    private static final DateTimeFormatter DATE_PREFIX = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public OrderNumberGenerator(Clock clock) {
        this.clock = clock;
    }

    public String generate() {
        var sb = new StringBuilder(19);
        sb.append(LocalDate.now(clock).format(DATE_PREFIX)).append('-');
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }

}
