package com.gdc.backend.registration.entity;

import com.gdc.backend.common.entity.AuditedEntity;
import com.gdc.backend.testmaster.entity.DiagnosticTest;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "registration_tests", uniqueConstraints = @UniqueConstraint(columnNames = {"registration_id", "test_id"}))
public class RegistrationTest extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "registration_id") private Registration registration;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "test_id") private DiagnosticTest test;
    @Column(name = "test_name_snapshot", nullable = false, length = 200) private String testNameSnapshot;
    @Column(name = "category_code_snapshot", nullable = false, length = 30) private String categoryCodeSnapshot;
    @Column(name = "category_name_snapshot", nullable = false, length = 100) private String categoryNameSnapshot;
    @Column(name = "test_price", nullable = false, precision = 10, scale = 2) private BigDecimal testPrice;
    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2) private BigDecimal discountPercentage;
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2) private BigDecimal discountAmount;
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2) private BigDecimal totalAmount;
    @Column(name = "display_order", nullable = false) private Integer displayOrder;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private TestStatus status;
    @Column(name = "registered_at", nullable = false) private Instant registeredAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "completed_by", length = 100) private String completedBy;

    public Long getId() { return id; }
    public Registration getRegistration() { return registration; }
    public void setRegistration(Registration value) { registration = value; }
    public DiagnosticTest getTest() { return test; }
    public void setTest(DiagnosticTest value) { test = value; }
    public String getTestNameSnapshot() { return testNameSnapshot; }
    public void setTestNameSnapshot(String value) { testNameSnapshot = value; }
    public String getCategoryCodeSnapshot() { return categoryCodeSnapshot; }
    public void setCategoryCodeSnapshot(String value) { categoryCodeSnapshot = value; }
    public String getCategoryNameSnapshot() { return categoryNameSnapshot; }
    public void setCategoryNameSnapshot(String value) { categoryNameSnapshot = value; }
    public BigDecimal getTestPrice() { return testPrice; }
    public void setTestPrice(BigDecimal value) { testPrice = value; }
    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(BigDecimal value) { discountPercentage = value; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal value) { discountAmount = value; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal value) { totalAmount = value; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer value) { displayOrder = value; }
    public TestStatus getStatus() { return status; }
    public void setStatus(TestStatus value) { status = value; }
    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant value) { registeredAt = value; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant value) { completedAt = value; }
    public String getCompletedBy() { return completedBy; }
    public void setCompletedBy(String value) { completedBy = value; }
}
