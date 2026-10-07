package com.gdc.backend.payment.entity;

import com.gdc.backend.common.entity.AuditedEntity;
import com.gdc.backend.registration.entity.Registration;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "payment_no", nullable = false, unique = true, length = 20) private String paymentNo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "registration_id") private Registration registration;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(name = "payment_mode", nullable = false, length = 10) private PaymentMode paymentMode;
    @Column(name = "payment_datetime", nullable = false) private Instant paymentDatetime;
    @Column(name = "reference_no", length = 100) private String referenceNo;
    @Column(length = 1000) private String remarks;
    @Column(name = "received_by", length = 100) private String receivedBy;

    public Long getId() { return id; }
    public String getPaymentNo() { return paymentNo; }
    public void setPaymentNo(String value) { paymentNo = value; }
    public Registration getRegistration() { return registration; }
    public void setRegistration(Registration value) { registration = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public PaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(PaymentMode value) { paymentMode = value; }
    public Instant getPaymentDatetime() { return paymentDatetime; }
    public void setPaymentDatetime(Instant value) { paymentDatetime = value; }
    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String value) { referenceNo = value; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String value) { remarks = value; }
    public String getReceivedBy() { return receivedBy; }
}
