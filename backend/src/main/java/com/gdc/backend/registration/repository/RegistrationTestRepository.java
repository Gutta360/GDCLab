package com.gdc.backend.registration.repository;

import com.gdc.backend.registration.entity.RegistrationTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface RegistrationTestRepository extends JpaRepository<RegistrationTest, Long> {
    @Query("SELECT rt FROM RegistrationTest rt JOIN FETCH rt.registration r JOIN FETCH rt.test t WHERE r.registrationNo = :registrationNo AND UPPER(t.testCode) = UPPER(:testCode)")
    Optional<RegistrationTest> findForStatusUpdate(@Param("registrationNo") String registrationNo,
                                                    @Param("testCode") String testCode);
}
