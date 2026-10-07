package com.gdc.backend.registration.repository;

import com.gdc.backend.common.dto.*;
import com.gdc.backend.registration.dto.*;
import com.gdc.backend.registration.entity.TestStatus;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public class RegistrationTestSearchRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public RegistrationTestSearchRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public PagedReportResponse<RegistrationTestSearchRow, TestSearchSummary> search(String testCode, String testName,
            String category, String registrationNo, String patientName, Long branchId, TestStatus status,
            LocalDate fromDate, LocalDate toDate, TestDateMode dateMode, int page, int size) {
        RegistrationSearchRepository.validatePage(page, size);
        TestDateMode mode = dateMode == null ? TestDateMode.REGISTERED : dateMode;
        String dateColumn = mode == TestDateMode.COMPLETED ? "rt.completed_at" : "rt.registered_at";
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        MapSqlParameterSource p = new MapSqlParameterSource();
        RegistrationSearchRepository.contains(where,p,"dt.test_code","testCode",testCode);
        RegistrationSearchRepository.contains(where,p,"rt.test_name_snapshot","testName",testName);
        if (category != null && !category.isBlank()) {
            where.append(" AND (LOWER(rt.category_code_snapshot) LIKE LOWER(:category) OR LOWER(rt.category_name_snapshot) LIKE LOWER(:category))");
            p.addValue("category", "%" + category.trim() + "%");
        }
        RegistrationSearchRepository.contains(where,p,"r.registration_no","registrationNo",registrationNo);
        RegistrationSearchRepository.contains(where,p,"r.patient_name_snapshot","patientName",patientName);
        if (branchId != null) { where.append(" AND r.branch_id=:branchId"); p.addValue("branchId",branchId); }
        if (status != null) { where.append(" AND rt.status=:status"); p.addValue("status",status.name()); }
        if (mode == TestDateMode.COMPLETED) where.append(" AND rt.completed_at IS NOT NULL");
        RegistrationSearchRepository.dateRange(where,p,dateColumn,fromDate,toDate);
        String from = " FROM gdclab.registration_tests rt JOIN gdclab.registrations r ON r.id=rt.registration_id JOIN gdclab.diagnostic_tests dt ON dt.id=rt.test_id";
        var summary = jdbc.queryForObject("""
                SELECT COUNT(*) total_tests,COALESCE(SUM(rt.test_price),0) gross,
                       COALESCE(SUM(rt.discount_amount),0) discount,COALESCE(SUM(rt.total_amount),0) net
                """ + from + where, p, (rs,n) -> new TestSearchSummary(rs.getLong("total_tests"),
                rs.getBigDecimal("gross"),rs.getBigDecimal("discount"),rs.getBigDecimal("net")));
        p.addValue("limit",size).addValue("offset",(long)page*size);
        String sql = """
                SELECT %s business_datetime,dt.test_code,rt.test_name_snapshot,
                       rt.category_code_snapshot,rt.category_name_snapshot,rt.status,
                       r.registration_no,r.bill_no,r.patient_name_snapshot,rt.test_price,
                       rt.discount_amount,rt.total_amount,r.branch_code_snapshot,r.branch_name_snapshot
                """.formatted(dateColumn) + from + where + " ORDER BY " + dateColumn + " DESC,rt.id DESC LIMIT :limit OFFSET :offset";
        var rows = jdbc.query(sql,p,(rs,n)->new RegistrationTestSearchRow(
                rs.getTimestamp("business_datetime").toInstant(),rs.getString("test_code"),rs.getString("test_name_snapshot"),
                rs.getString("category_code_snapshot"),rs.getString("category_name_snapshot"),TestStatus.valueOf(rs.getString("status")),
                rs.getString("registration_no"),rs.getString("bill_no"),rs.getString("patient_name_snapshot"),
                rs.getBigDecimal("test_price"),rs.getBigDecimal("discount_amount"),rs.getBigDecimal("total_amount"),
                rs.getString("branch_code_snapshot"),rs.getString("branch_name_snapshot")));
        return new PagedReportResponse<>(PageResponse.of(rows,page,size,summary.totalTests()),summary);
    }
}
