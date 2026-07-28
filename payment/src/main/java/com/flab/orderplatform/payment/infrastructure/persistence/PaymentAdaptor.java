package com.flab.orderplatform.payment.infrastructure.persistence;

import com.flab.orderplatform.payment.application.PaymentRepository;
import com.flab.orderplatform.payment.domain.Payment;
import com.flab.orderplatform.payment.domain.status.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}
