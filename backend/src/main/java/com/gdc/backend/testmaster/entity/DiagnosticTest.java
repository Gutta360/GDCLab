package com.gdc.backend.testmaster.entity;

import com.gdc.backend.common.entity.AuditedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "diagnostic_tests")
public class DiagnosticTest extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "test_code", nullable = false, unique = true, length = 20) private String testCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "category_id") private TestCategory category;
    @Column(name = "test_name", nullable = false, length = 200) private String testName;
    @Column(name = "current_price", nullable = false, precision = 10, scale = 2) private BigDecimal currentPrice;
    @Column(name = "display_order", nullable = false) private Integer displayOrder;
    @Column(name = "is_active", nullable = false) private boolean active = true;

    public Long getId() { return id; }
    public String getTestCode() { return testCode; }
    public TestCategory getCategory() { return category; }
    public String getTestName() { return testName; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public Integer getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
}
