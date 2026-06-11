package com.back.sportteam.domain.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.back.sportteam.domain.payment.dto.request.PaymentPrepareRequest;
import com.back.sportteam.domain.payment.dto.response.PaymentPrepareResponse;
import com.back.sportteam.domain.payment.entity.Payment;
import com.back.sportteam.domain.payment.entity.PaymentType;
import com.back.sportteam.domain.payment.repository.PaymentRepository;
import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.exception.errorcode.PaymentErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ObjectProvider<PaymentAmountReader> paymentAmountReaderProvider;

    @Mock
    private PaymentAmountReader paymentAmountReader;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, paymentAmountReaderProvider);
    }

    @Test
    void prepareCreatesFacilityPaymentWhenAmountMatches() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                101L,
                100_000L,
                PaymentType.FACILITY
        );
        when(paymentAmountReaderProvider.getIfAvailable()).thenReturn(paymentAmountReader);
        when(paymentAmountReader.getFacilityAmount(101L)).thenReturn(100_000L);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentPrepareResponse response = paymentService.prepare(request);

        assertNotNull(response.merchantUid());
        assertEquals(100_000L, response.amount());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void prepareRejectsTamperedAmount() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                50L,
                1_000L,
                PaymentType.PARTICIPATION
        );
        when(paymentAmountReaderProvider.getIfAvailable()).thenReturn(paymentAmountReader);
        when(paymentAmountReader.getParticipationAmount(50L)).thenReturn(10_000L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> paymentService.prepare(request)
        );

        assertEquals(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH, exception.getErrorCode());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void prepareRejectsPaymentWithoutMatchId() {
        PaymentPrepareRequest request = new PaymentPrepareRequest(
                null,
                100_000L,
                PaymentType.FACILITY
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> paymentService.prepare(request)
        );

        assertEquals(PaymentErrorCode.INVALID_PAYMENT_REQUEST, exception.getErrorCode());
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
