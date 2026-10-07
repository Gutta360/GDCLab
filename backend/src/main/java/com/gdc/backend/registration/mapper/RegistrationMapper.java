package com.gdc.backend.registration.mapper;

import com.gdc.backend.payment.dto.PaymentResponse;
import com.gdc.backend.payment.entity.Payment;
import com.gdc.backend.registration.dto.*;
import com.gdc.backend.registration.entity.*;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class RegistrationMapper {
    public RegistrationTestResponse test(RegistrationTest t) {
        return new RegistrationTestResponse(t.getTest().getTestCode(),t.getTestNameSnapshot(),t.getCategoryCodeSnapshot(),
                t.getCategoryNameSnapshot(),t.getTestPrice(),t.getDiscountPercentage(),t.getDiscountAmount(),
                t.getTotalAmount(),t.getDisplayOrder(),t.getStatus(),t.getRegisteredAt(),t.getCompletedAt(),t.getVersion());
    }
    public PaymentResponse payment(Payment p) {
        return new PaymentResponse(p.getPaymentNo(),p.getAmount(),p.getPaymentMode(),p.getPaymentDatetime(),
                p.getReferenceNo(),p.getRemarks(),p.getReceivedBy());
    }
    public RegistrationResponse response(Registration r,List<Payment> payments) {
        return new RegistrationResponse(r.getRegistrationNo(),r.getBillNo(),r.getRegistrationDatetime(),
                r.getPatient().getPatientId(),r.getPatientTitleSnapshot(),r.getPatientNameSnapshot(),r.getPatientAgeSnapshot(),
                r.getPatientGenderSnapshot(),r.getPatientMobileSnapshot(),r.getPatientAddressSnapshot(),
                r.getBranch().getId(),r.getBranchCodeSnapshot(),r.getBranchNameSnapshot(),
                r.getReferringDoctor()==null?null:r.getReferringDoctor().getId(),r.getReferringDoctorNameSnapshot(),
                r.getReferringDoctorQualificationSnapshot(),r.getReferringLab()==null?null:r.getReferringLab().getId(),
                r.getReferringLabNameSnapshot(),r.getTests().stream().map(this::test).toList(),r.getBillAmount(),
                r.getDiscountPercentage(),r.getDiscountAmount(),r.getFinalBillAmount(),r.getPaidAmount(),r.getDueAmount(),
                r.getStatus(),r.getRemarks(),payments.stream().map(this::payment).toList(),r.getVersion());
    }
    public BillResponse bill(Registration r) {
        var lines = new java.util.ArrayList<BillResponse.BillTestLine>();
        int sequence=1;
        for (RegistrationTest t:r.getTests()) lines.add(new BillResponse.BillTestLine(sequence++,t.getTestNameSnapshot(),
                t.getTestPrice(),t.getDiscountPercentage(),t.getDiscountAmount(),t.getTotalAmount()));
        return new BillResponse(r.getBranchNameSnapshot(),r.getBranchAddressSnapshot(),r.getBranchPhoneSnapshot(),
                r.getBranchMobileSnapshot(),r.getRegistrationNo(),r.getBillNo(),r.getRegistrationDatetime(),
                r.getPatientTitleSnapshot(),r.getPatientNameSnapshot(),r.getPatientAgeSnapshot(),r.getPatientGenderSnapshot(),
                r.getPatientMobileSnapshot(),r.getPatientAddressSnapshot(),r.getReferringDoctorNameSnapshot(),
                r.getReferringDoctorQualificationSnapshot(),r.getReferringLabNameSnapshot(),lines,r.getBillAmount(),
                r.getDiscountPercentage(),r.getDiscountAmount(),r.getFinalBillAmount(),r.getPaidAmount(),r.getDueAmount(),r.getCreatedBy());
    }
}
