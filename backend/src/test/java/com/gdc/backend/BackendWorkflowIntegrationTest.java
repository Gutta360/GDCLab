package com.gdc.backend;

import com.gdc.backend.common.exception.BusinessValidationException;
import com.gdc.backend.payment.dto.PaymentCreateRequest;
import com.gdc.backend.payment.entity.PaymentMode;
import com.gdc.backend.payment.service.PaymentService;
import com.gdc.backend.registration.dto.*;
import com.gdc.backend.registration.entity.*;
import com.gdc.backend.registration.service.*;
import com.gdc.backend.support.PostgresIntegrationTest;
import com.gdc.backend.testmaster.service.DiagnosticTestService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@Transactional
class BackendWorkflowIntegrationTest extends PostgresIntegrationTest {
    @Autowired RegistrationService registrations;
    @Autowired RegistrationTestSearchService testSearch;
    @Autowired DiagnosticTestService diagnosticTests;
    @Autowired PaymentService payments;
    @Autowired JdbcTemplate jdbc;
    Long branchId;
    Long doctorId;
    Long labId;
    String patientMobile;

    @BeforeEach void masters() {
        patientMobile="9"+String.format("%09d",Math.floorMod(System.nanoTime(),1_000_000_000L));
        branchId=jdbc.queryForObject("SELECT id FROM gdclab.branches WHERE branch_code='SBD-NANDIGAMA'",Long.class);
        doctorId=insertReferral("DOC-TEST-"+System.nanoTime(),"DOCTOR","Dr Test");
        labId=insertReferral("LAB-TEST-"+System.nanoTime(),"LAB","Test Lab");
    }

    @Test void supportsAllReferralCombinationsAndIndependentRepeatVisits() {
        var doctor=registrations.create(request(doctorId,null,List.of(test("TST-00022",0)),0,null));
        var doctorLab=registrations.create(request(doctorId,labId,List.of(test("TST-00001",0)),0,null));
        var lab=registrations.create(request(null,labId,List.of(test("TST-00002",0)),0,null));
        var self=registrations.create(request(null,null,List.of(test("TST-00003",0)),0,null));
        assertThat(List.of(doctor,doctorLab,lab,self)).extracting(RegistrationResponse::patientName).containsOnly("Workflow Person");
        assertThat(doctorLab.referringDoctorName()).isEqualTo("Dr Test");
        assertThat(doctorLab.referringLabName()).isEqualTo("Test Lab");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM gdclab.registrations WHERE patient_mobile=?",Long.class,patientMobile)).isEqualTo(4);
    }

    @Test void loadsActiveDiagnosticTestsWithoutOptionalFilters() {
        var results = diagnosticTests.search(null, null, true);

        assertThat(results).hasSize(41);
        assertThat(results).allMatch(test -> test.active() && test.categoryCode() != null);
        assertThat(results.getFirst().testCode()).isEqualTo("TST-00001");
    }

    @Test void rejectsInactiveMastersWrongReferralTypesAndInvalidTestSelection() {
        Long inactiveBranch=jdbc.queryForObject("INSERT INTO gdclab.branches(branch_code,branch_name,address,is_active) VALUES ('INACTIVE','Inactive','Address',false) RETURNING id",Long.class);
        assertThatThrownBy(()->registrations.create(request(doctorId,null,List.of(test("TST-00022",0)),0,null, inactiveBranch))).isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(()->registrations.create(request(labId,null,List.of(test("TST-00022",0)),0,null))).isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(()->registrations.create(request(null,null,List.of(),0,null))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->registrations.create(request(null,null,List.of(test("TST-00022",0),test("tst-00022",0)),0,null))).isInstanceOf(BusinessValidationException.class);
        jdbc.update("UPDATE gdclab.diagnostic_tests SET is_active=false WHERE test_code='TST-00041'");
        assertThatThrownBy(()->registrations.create(request(null,null,List.of(test("TST-00041",0)),0,null))).isInstanceOf(BusinessValidationException.class);
    }

    @Test void usesMasterPricesAppliesDiscountsAndPreservesSnapshots() {
        var response=registrations.create(request(doctorId,labId,
                List.of(test("TST-00022",10),test("TST-00001",0)),5,
                new PaymentCreateRequest(new BigDecimal("1000"),PaymentMode.CASH,null,null)));
        assertThat(response.billAmount()).isEqualByComparingTo("3900.00");
        assertThat(response.discountAmount()).isEqualByComparingTo("195.00");
        assertThat(response.finalBillAmount()).isEqualByComparingTo("3705.00");
        assertThat(response.paidAmount()).isEqualByComparingTo("1000.00");
        assertThat(response.dueAmount()).isEqualByComparingTo("2705.00");
        assertThat(response.status()).isEqualTo(RegistrationStatus.PARTIALLY_PAID);
        jdbc.update("UPDATE gdclab.diagnostic_tests SET test_name='Changed',current_price=1200 WHERE test_code='TST-00022'");
        jdbc.update("UPDATE gdclab.branches SET branch_name='Changed Branch' WHERE id=?",branchId);
        jdbc.update("UPDATE gdclab.referral_sources SET name='Changed Doctor' WHERE id=?",doctorId);
        var bill=registrations.bill(response.registrationNo());
        assertThat(bill.branchName()).isEqualTo("SRI BALAJI DIAGNOSTICS");
        assertThat(bill.referringDoctorName()).isEqualTo("Dr Test");
        assertThat(bill.tests()).extracting(BillResponse.BillTestLine::testName).contains("Pregnancy (OBG)");
        assertThat(bill.tests()).extracting(BillResponse.BillTestLine::testAmount).contains(new BigDecimal("1000.00"));
        assertThat(BillResponse.class.getRecordComponents()).extracting(java.lang.reflect.RecordComponent::getName).noneMatch(n->n.toLowerCase().contains("aadhaar"));
    }

    @Test void supportsPartialMultipleAndFullPaymentsAndRejectsOverpayment() {
        var r=registrations.create(request(null,null,List.of(test("TST-00001",0)),0,null));
        var first=payments.create(r.registrationNo(),new PaymentCreateRequest(new BigDecimal("1000"),PaymentMode.CASH,null,null));
        assertThat(first.paymentNo()).startsWith("PAY-");
        assertThat(registrations.get(r.registrationNo()).status()).isEqualTo(RegistrationStatus.PARTIALLY_PAID);
        payments.create(r.registrationNo(),new PaymentCreateRequest(new BigDecimal("2000"),PaymentMode.UPI,"upi-ref",null));
        var paid=registrations.get(r.registrationNo());
        assertThat(paid.status()).isEqualTo(RegistrationStatus.PAID);
        assertThat(paid.payments()).hasSize(2);
        assertThatThrownBy(()->payments.create(r.registrationNo(),new PaymentCreateRequest(new BigDecimal("1"),PaymentMode.CASH,null,null)))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test void enforcesTestLifecycleAndServerCompletionTime() {
        var r=registrations.create(request(null,null,List.of(test("TST-00022",0)),0,null));
        var line=r.tests().getFirst();
        var inProgress=registrations.updateTestStatus(r.registrationNo(),"TST-00022",new TestStatusUpdateRequest(TestStatus.IN_PROGRESS,line.version()));
        var completed=registrations.updateTestStatus(r.registrationNo(),"TST-00022",new TestStatusUpdateRequest(TestStatus.COMPLETED,inProgress.version()));
        assertThat(completed.completedAt()).isNotNull();
        LocalDate completedDate=completed.completedAt().atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
        var conducted=testSearch.search("TST-00022",null,null,r.registrationNo(),null,branchId,TestStatus.COMPLETED,
                completedDate,completedDate,TestDateMode.COMPLETED,0,25);
        assertThat(conducted.summary().totalTests()).isEqualTo(1);
        assertThatThrownBy(()->registrations.updateTestStatus(r.registrationNo(),"TST-00022",new TestStatusUpdateRequest(TestStatus.REGISTERED,completed.version())))
                .isInstanceOf(BusinessValidationException.class);
        var cancellable=registrations.create(request(null,null,List.of(test("TST-00023",0)),0,null));
        var cancelled=registrations.updateTestStatus(cancellable.registrationNo(),"TST-00023",
                new TestStatusUpdateRequest(TestStatus.CANCELLED,cancellable.tests().getFirst().version()));
        assertThat(cancelled.status()).isEqualTo(TestStatus.CANCELLED);
        assertThat(cancelled.completedAt()).isNull();
    }

    @Test void filtersAndAggregatesCompleteResultsRatherThanCurrentPage() {
        Instant businessTime=LocalDate.of(2026,10,1).atTime(10,0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();
        var r=registrations.create(new RegistrationCreateRequest("Mrs","Workflow Person",patientMobile,"123456789012",
                Gender.FEMALE,"workflow@example.com",LocalDate.of(2006,1,1),(short)20,null,branchId,null,null,businessTime,
                List.of(test("TST-00022",10),test("TST-00001",0)),BigDecimal.ZERO,null,null));
        var page=registrations.search(r.registrationNo(),null,"Workflow",patientMobile,branchId,
                LocalDate.of(2026,10,1),LocalDate.of(2026,10,1),0,1);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(registrations.search(null,r.billNo(),null,null,null,null,null,0,25).totalElements()).isEqualTo(1);
        var report=testSearch.search(null,"Pregnancy","ULTRASOUND",r.registrationNo(),"Workflow",branchId,
                TestStatus.REGISTERED,LocalDate.of(2026,10,1),LocalDate.of(2026,10,1),TestDateMode.REGISTERED,0,1);
        assertThat(report.summary().totalTests()).isEqualTo(1);
        assertThat(report.summary().grossAmount()).isEqualByComparingTo("1000.00");
        assertThat(testSearch.search("TST-00001",null,"CT_SCAN",null,null,null,null,null,null,TestDateMode.REGISTERED,0,25)
                .summary().totalTests()).isEqualTo(1);
        payments.create(r.registrationNo(),new PaymentCreateRequest(new BigDecimal("200"),PaymentMode.CASH,null,null));
        payments.create(r.registrationNo(),new PaymentCreateRequest(new BigDecimal("300"),PaymentMode.UPI,null,null));
        var paymentReport=payments.search(null,r.billNo(),r.registrationNo(),"Workflow",patientMobile,null,branchId,
                LocalDate.now(ZoneId.of("Asia/Kolkata")),LocalDate.now(ZoneId.of("Asia/Kolkata")),0,1);
        assertThat(paymentReport.page().content()).hasSize(1);
        assertThat(paymentReport.summary().paymentCount()).isEqualTo(2);
        assertThat(paymentReport.summary().grandTotal()).isEqualByComparingTo("500.00");
        assertThat(payments.search(null,null,null,"Workflow",null,PaymentMode.CASH,null,null,null,0,25).summary().cashTotal())
                .isEqualByComparingTo("200.00");
    }

    @Test
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void rollsBackRegistrationWhenLateValidationFails() {
        long registrationsBefore=jdbc.queryForObject("SELECT COUNT(*) FROM gdclab.registrations",Long.class);
        assertThatThrownBy(()->registrations.create(request(null,null,List.of(test("TST-00022",0)),0,
                new PaymentCreateRequest(new BigDecimal("1001"),PaymentMode.CASH,null,null)))).isInstanceOf(BusinessValidationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM gdclab.registrations",Long.class)).isEqualTo(registrationsBefore);
    }

    @Test
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void pessimisticLockPreventsConcurrentOverpayment() throws Exception {
        var r=registrations.create(request(null,null,List.of(test("TST-00001",0)),0,null));
        CountDownLatch start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            Callable<Boolean> attempt=()->{
                start.await(5,TimeUnit.SECONDS);
                try {
                    payments.create(r.registrationNo(),new PaymentCreateRequest(new BigDecimal("3000"),PaymentMode.CASH,null,null));
                    return true;
                } catch (BusinessValidationException exception) {
                    return false;
                }
            };
            Future<Boolean> first=executor.submit(attempt);Future<Boolean> second=executor.submit(attempt);start.countDown();
            assertThat(List.of(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
        var result=registrations.get(r.registrationNo());
        assertThat(result.paidAmount()).isEqualByComparingTo("3000.00");
        assertThat(result.dueAmount()).isZero();
        assertThat(result.payments()).hasSize(1);
    }

    private Long insertReferral(String code,String type,String name){return jdbc.queryForObject(
            "INSERT INTO gdclab.referral_sources(referral_code,referral_type,name) VALUES (?,?,?) RETURNING id",Long.class,code,type,name);}
    private RegistrationTestRequest test(String code,int discount){return new RegistrationTestRequest(code,new BigDecimal(discount));}
    private RegistrationCreateRequest request(Long doctor,Long lab,List<RegistrationTestRequest> tests,int discount,PaymentCreateRequest payment){
        return request(doctor,lab,tests,discount,payment,branchId);
    }
    private RegistrationCreateRequest request(Long doctor,Long lab,List<RegistrationTestRequest> tests,int discount,PaymentCreateRequest payment,Long selectedBranch){
        return new RegistrationCreateRequest("Mrs","Workflow Person",patientMobile,"123456789012",Gender.FEMALE,
                "workflow@example.com",LocalDate.of(2006,1,1),(short)20,null,selectedBranch,doctor,lab,null,
                tests,new BigDecimal(discount),payment,null);
    }
}
