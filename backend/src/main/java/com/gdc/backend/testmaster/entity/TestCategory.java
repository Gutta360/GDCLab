package com.gdc.backend.testmaster.entity;

import com.gdc.backend.common.entity.AuditedEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "test_categories")
public class TestCategory extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "category_code", nullable = false, unique = true, length = 30) private String categoryCode;
    @Column(name = "category_name", nullable = false, length = 100) private String categoryName;
    @Column(name = "display_order", nullable = false) private Integer displayOrder;
    @Column(name = "is_active", nullable = false) private boolean active = true;

    public Long getId() { return id; }
    public String getCategoryCode() { return categoryCode; }
    public String getCategoryName() { return categoryName; }
    public Integer getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
}
