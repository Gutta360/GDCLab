package com.gdc.backend.patient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gdc.backend.patient.entity.Patient;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientId(String patientId);

    Optional<Patient> findByPatientIdAndActiveTrue(String patientId);

    boolean existsByPatientId(String patientId);

    List<Patient> findAllByActiveTrue();

    @Query(
            value = """
            SELECT *
            FROM patients p
            WHERE p.is_active = true
              AND (
                  LOWER(p.patient_id) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(p.first_name) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(COALESCE(p.last_name, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR p.mobile LIKE CONCAT('%', :query, '%')
              )
            ORDER BY p.first_name ASC, p.last_name ASC, p.patient_id ASC
            LIMIT 20
            """,
            nativeQuery = true
    )
    List<Patient> searchActivePatients(
            @Param("query") String query
    );

    @Query("""
            SELECT COUNT(p) > 0
            FROM Patient p
            WHERE LOWER(TRIM(p.firstName)) = LOWER(TRIM(:firstName))
              AND p.mobile = :mobile
            """)
    boolean existsByFirstNameAndMobile(
            @Param("firstName") String firstName,
            @Param("mobile") String mobile
    );

    @Query("""
            SELECT COUNT(p) > 0
            FROM Patient p
            WHERE LOWER(TRIM(p.firstName)) = LOWER(TRIM(:firstName))
              AND p.mobile = :mobile
              AND p.patientId <> :patientId
            """)
    boolean existsByFirstNameAndMobileAndPatientIdNot(
            @Param("firstName") String firstName,
            @Param("mobile") String mobile,
            @Param("patientId") String patientId
    );

    @Query(
            value = "SELECT nextval('patient_number_seq')",
            nativeQuery = true
    )
    Long getNextPatientNumber();
}
