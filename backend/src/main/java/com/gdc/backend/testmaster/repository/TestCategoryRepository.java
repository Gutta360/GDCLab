package com.gdc.backend.testmaster.repository;

import com.gdc.backend.testmaster.entity.TestCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestCategoryRepository extends JpaRepository<TestCategory, Long> {
    List<TestCategory> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();
}
