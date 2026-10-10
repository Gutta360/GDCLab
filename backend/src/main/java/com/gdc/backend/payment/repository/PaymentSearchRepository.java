package com.gdc.backend.payment.repository;

import com.gdc.backend.common.dto.*;
import com.gdc.backend.payment.dto.*;
import com.gdc.backend.payment.entity.PaymentMode;
import com.gdc.backend.registration.repository.RegistrationSearchRepository;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;

@Repository
public class PaymentSearchRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public PaymentSearchRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    public PagedReportResponse<PaymentSearchRow,PaymentSearchSummary> search(String paymentNo,String billNo,
            String registrationNo,String patientName,String mobile,PaymentMode mode,Long branchId,
            LocalDate fromDate,LocalDate toDate,int page,int size) {
        RegistrationSearchRepository.validatePage(page,size);
        StringBuilder where=new StringBuilder(" WHERE 1=1");
        MapSqlParameterSource p=new MapSqlParameterSource();
        RegistrationSearchRepository.contains(where,p,"p.payment_no","paymentNo",paymentNo);
        RegistrationSearchRepository.contains(where,p,"r.bill_no","billNo",billNo);
        RegistrationSearchRepository.contains(where,p,"r.registration_no","registrationNo",registrationNo);
        RegistrationSearchRepository.contains(where,p,"r.patient_name","patientName",patientName);
        RegistrationSearchRepository.contains(where,p,"r.patient_mobile","mobile",mobile);
        if(mode!=null){where.append(" AND p.payment_mode=:mode");p.addValue("mode",mode.name());}
        if(branchId!=null){where.append(" AND r.branch_id=:branchId");p.addValue("branchId",branchId);}
        RegistrationSearchRepository.dateRange(where,p,"p.payment_datetime",fromDate,toDate);
        String from=" FROM gdclab.payments p JOIN gdclab.registrations r ON r.id=p.registration_id";
        var summary=jdbc.queryForObject("""
                SELECT COUNT(*) payment_count,
                COALESCE(SUM(p.amount) FILTER(WHERE p.payment_mode='CASH'),0) cash_total,
                COALESCE(SUM(p.amount) FILTER(WHERE p.payment_mode='UPI'),0) upi_total,
                COALESCE(SUM(p.amount) FILTER(WHERE p.payment_mode='CARD'),0) card_total,
                COALESCE(SUM(p.amount) FILTER(WHERE p.payment_mode='OTHER'),0) other_total,
                COALESCE(SUM(p.amount),0) grand_total
                """+from+where,p,(rs,n)->new PaymentSearchSummary(rs.getLong("payment_count"),rs.getBigDecimal("cash_total"),
                rs.getBigDecimal("upi_total"),rs.getBigDecimal("card_total"),rs.getBigDecimal("other_total"),rs.getBigDecimal("grand_total")));
        p.addValue("limit",size).addValue("offset",(long)page*size);
        String sql="""
                SELECT p.payment_no,p.payment_datetime,r.registration_no,r.bill_no,r.patient_name,
                       r.patient_mobile,p.payment_mode,p.amount,p.received_by,
                       r.branch_code_snapshot,r.branch_name_snapshot
                """+from+where+" ORDER BY p.payment_datetime DESC,p.id DESC LIMIT :limit OFFSET :offset";
        var rows=jdbc.query(sql,p,(rs,n)->new PaymentSearchRow(rs.getString("payment_no"),rs.getTimestamp("payment_datetime").toInstant(),
                rs.getString("registration_no"),rs.getString("bill_no"),rs.getString("patient_name"),
                rs.getString("patient_mobile"),PaymentMode.valueOf(rs.getString("payment_mode")),rs.getBigDecimal("amount"),
                rs.getString("received_by"),rs.getString("branch_code_snapshot"),rs.getString("branch_name_snapshot")));
        return new PagedReportResponse<>(PageResponse.of(rows,page,size,summary.paymentCount()),summary);
    }
}
