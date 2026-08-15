package com.flab.orderplatform.payment.infrastructure.persistence;

import com.flab.orderplatform.payment.application.port.out.PaymentRepository;
import com.flab.orderplatform.payment.domain.Payment;
import com.flab.orderplatform.payment.domain.status.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryAdaptor implements PaymentRepository {
    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(Payment payment) {
        return paymentJpaRepository.save(payment);
    }

    @Override
    public Optional<Payment> findByOrderNumberAndStatus(String orderNumber, PaymentStatus paymentStatus) {
        return paymentJpaRepository.findByOrderNumberAndStatus(orderNumber, paymentStatus);
    }

    @Override
    public Optional<Payment> findByOrderNumberAndStatusIn(String orderNumber, List<PaymentStatus> paymentStatuses) {
        return paymentJpaRepository.findByOrderNumberAndStatusIn(orderNumber, paymentStatuses);
    }

    @Override
    public List<Payment> findByStatusAndPgRequestedAtBefore(PaymentStatus status, LocalDateTime threshold, Pageable pageable) {
        return paymentJpaRepository.findByStatusAndPgRequestedAtBefore(status, threshold, pageable);
    }
}
