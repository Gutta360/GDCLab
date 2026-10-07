package com.gdc.backend.registration.service;

import com.gdc.backend.common.dto.PagedReportResponse;
import com.gdc.backend.registration.dto.*;
import com.gdc.backend.registration.entity.TestStatus;
import com.gdc.backend.registration.repository.RegistrationTestSearchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@Transactional(readOnly=true)
public class RegistrationTestSearchService {
    private final RegistrationTestSearchRepository repository;
    public RegistrationTestSearchService(RegistrationTestSearchRepository repository){this.repository=repository;}
    public PagedReportResponse<RegistrationTestSearchRow,TestSearchSummary> search(String testCode,String testName,
            String category,String registrationNo,String patientName,Long branchId,TestStatus status,
            LocalDate fromDate,LocalDate toDate,TestDateMode dateMode,int page,int size){
        return repository.search(testCode,testName,category,registrationNo,patientName,branchId,status,fromDate,toDate,dateMode,page,size);
    }
}
