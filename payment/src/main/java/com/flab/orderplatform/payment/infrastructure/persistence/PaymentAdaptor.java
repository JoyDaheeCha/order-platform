package com.flab.orderplatform.payment.infrastructure.persistence;

import com.flab.orderplatform.payment.application.port.out.PaymentRepository;
import com.flab.orderplatform.payment.domain.Payment;
import com.flab.orderplatform.payment.domain.status.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentAdaptor implements PaymentRepository {
    private final PaymentJapRepository paymentJapRepository;

    @Override
    public Payment save(Payment payment) {
        return paymentJapRepository.save(payment);
    }

    @Override
    public Optional<Payment> findByOrderNumberAndStatus(String orderNumber, PaymentStatus paymentStatus) {
        return paymentJapRepository.findByOrderNumberAndStatus(orderNumber, paymentStatus);
    }

    @Override
    public Optional<Payment> findByOrderNumberAndStatusIn(String orderNumber, List<PaymentStatus> paymentStatuses) {
        return paymentJapRepository.findByOrderNumberAndStatusIn(orderNumber, paymentStatuses);
    }
}
