package com.gdc.backend.testmaster.repository;

import com.gdc.backend.testmaster.entity.DiagnosticTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DiagnosticTestRepository extends JpaRepository<DiagnosticTest, Long> {
    @Query("SELECT t FROM DiagnosticTest t JOIN FETCH t.category WHERE UPPER(t.testCode) = UPPER(:code)")
    Optional<DiagnosticTest> findByTestCodeWithCategory(@Param("code") String code);

    @Query("""
            SELECT t FROM DiagnosticTest t JOIN FETCH t.category c
            WHERE (:filterByActive = false OR t.active = :active)
              AND (:category = '' OR UPPER(c.categoryCode) = UPPER(:category))
              AND (:query = '' OR LOWER(t.testCode) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(t.testName) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY c.displayOrder, t.displayOrder, t.id
            """)
    List<DiagnosticTest> search(@Param("query") String query, @Param("category") String category,
                                @Param("active") boolean active,
                                @Param("filterByActive") boolean filterByActive);
}
