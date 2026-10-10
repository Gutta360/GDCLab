package com.gdc.backend.registration.repository;

import com.gdc.backend.registration.entity.Registration;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    @EntityGraph(attributePaths = {"branch", "referringDoctor", "referringLab", "tests", "tests.test", "tests.test.category"})
    Optional<Registration> findByRegistrationNo(String registrationNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Registration r WHERE r.registrationNo = :registrationNo")
    Optional<Registration> findByRegistrationNoForUpdate(@Param("registrationNo") String registrationNo);

    @Query(value = "SELECT nextval('gdclab.registration_number_seq')", nativeQuery = true)
    Long nextRegistrationNumber();

    @Query(value = "SELECT nextval('gdclab.bill_number_seq')", nativeQuery = true)
    Long nextBillNumber();
}
