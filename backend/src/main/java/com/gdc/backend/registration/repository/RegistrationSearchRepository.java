package com.gdc.backend.registration.repository;

import com.gdc.backend.common.dto.PageResponse;
import com.gdc.backend.common.search.BusinessDateRange;
import com.gdc.backend.registration.dto.RegistrationSearchRow;
import com.gdc.backend.registration.entity.RegistrationStatus;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

@Repository
public class RegistrationSearchRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public RegistrationSearchRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public PageResponse<RegistrationSearchRow> search(String registrationNo, String billNo, String patientId,
            String patientName, String mobile, Long branchId, LocalDate fromDate, LocalDate toDate, int page, int size) {
        validatePage(page, size);
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        MapSqlParameterSource p = new MapSqlParameterSource();
        contains(where, p, "r.registration_no", "registrationNo", registrationNo);
        contains(where, p, "r.bill_no", "billNo", billNo);
        contains(where, p, "p.patient_id", "patientId", patientId);
        contains(where, p, "r.patient_name_snapshot", "patientName", patientName);
        contains(where, p, "r.patient_mobile_snapshot", "mobile", mobile);
        if (branchId != null) { where.append(" AND r.branch_id = :branchId"); p.addValue("branchId", branchId); }
        dateRange(where, p, "r.registration_datetime", fromDate, toDate);
        String from = " FROM gdclab.registrations r JOIN gdclab.patients p ON p.id=r.patient_id JOIN gdclab.branches b ON b.id=r.branch_id";
        long total = jdbc.queryForObject("SELECT COUNT(*)" + from + where, p, Long.class);
        p.addValue("limit", size).addValue("offset", (long) page * size);
        String sql = """
                SELECT r.registration_no,r.bill_no,r.registration_datetime,p.patient_id,
                       r.patient_name_snapshot,r.patient_mobile_snapshot,
                       (SELECT COUNT(*) FROM gdclab.registration_tests rt WHERE rt.registration_id=r.id) test_count,
                       r.bill_amount,r.paid_amount,r.due_amount,r.status,
                       r.branch_code_snapshot,r.branch_name_snapshot
                """ + from + where + " ORDER BY r.registration_datetime DESC,r.id DESC LIMIT :limit OFFSET :offset";
        var rows = jdbc.query(sql, p, (rs, n) -> new RegistrationSearchRow(
                rs.getString("registration_no"), rs.getString("bill_no"), rs.getTimestamp("registration_datetime").toInstant(),
                rs.getString("patient_id"), rs.getString("patient_name_snapshot"), rs.getString("patient_mobile_snapshot"),
                rs.getLong("test_count"), rs.getBigDecimal("bill_amount"), rs.getBigDecimal("paid_amount"),
                rs.getBigDecimal("due_amount"), RegistrationStatus.valueOf(rs.getString("status")),
                rs.getString("branch_code_snapshot"), rs.getString("branch_name_snapshot")));
        return PageResponse.of(rows, page, size, total);
    }

    public static void contains(StringBuilder where, MapSqlParameterSource p, String column, String name, String value) {
        if (value != null && !value.isBlank()) {
            where.append(" AND LOWER(").append(column).append(") LIKE LOWER(:").append(name).append(")");
            p.addValue(name, "%" + value.trim() + "%");
        }
    }
    public static void dateRange(StringBuilder where, MapSqlParameterSource p, String column, LocalDate from, LocalDate to) {
        BusinessDateRange range = BusinessDateRange.from(from, to);
        if (range.fromInclusive() != null) {
            where.append(" AND ").append(column).append(" >= :fromInstant AND ").append(column).append(" < :toInstant");
            p.addValue("fromInstant", Timestamp.from(range.fromInclusive()));
            p.addValue("toInstant", Timestamp.from(range.toExclusive()));
        }
    }
    public static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 200) throw new com.gdc.backend.common.exception.BusinessValidationException("Page must be >= 0 and size must be 1..200");
    }
}
