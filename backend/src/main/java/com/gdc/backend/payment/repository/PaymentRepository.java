package com.gdc.backend.payment.repository;

import com.gdc.backend.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByRegistrationIdOrderByPaymentDatetimeAscIdAsc(Long registrationId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.registration.id = :registrationId")
    BigDecimal sumByRegistrationId(@Param("registrationId") Long registrationId);

    @Query(value = "SELECT nextval('gdclab.payment_number_seq')", nativeQuery = true)
    Long nextPaymentNumber();
}
