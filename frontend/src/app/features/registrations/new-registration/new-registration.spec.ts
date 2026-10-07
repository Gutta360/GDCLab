import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NewRegistration } from './new-registration';
import { DiagnosticTestResponse } from '../../diagnostics/models/lab.model';

describe('NewRegistration', () => {
  let component: NewRegistration;
  let fixture: ComponentFixture<NewRegistration>;
  let http: HttpTestingController;

  const test: DiagnosticTestResponse = {
    testCode: 'TST-00001', testName: 'CT Test', categoryCode: 'CT_SCAN', categoryName: 'CT Scan',
    currentPrice: 1000, displayOrder: 1, active: true
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NewRegistration],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(NewRegistration);
    component = fixture.componentInstance;
    fixture.detectChanges();

    http.expectOne('api/v1/branches').flush([{ id: 1, branchCode: 'MAIN', branchName: 'Main', address: 'Address', phone: null, mobile: null }]);
    http.expectOne(request => request.url === 'api/v1/referral-sources' && request.params.get('type') === 'DOCTOR').flush([]);
    http.expectOne(request => request.url === 'api/v1/referral-sources' && request.params.get('type') === 'LAB').flush([]);
    http.expectOne(request => request.url === 'api/v1/tests' && request.params.get('active') === 'true').flush([test]);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('prevents duplicate test selection and calculates discounts', () => {
    component.addTest(test);
    component.addTest(test);
    expect(component.selectedTests).toHaveLength(1);

    component.setTestDiscount(component.selectedTests[0], '10');
    component.form.patchValue({ billDiscountPercentage: 5 });
    expect(component.lineDiscount(component.selectedTests[0])).toBe(100);
    expect(component.billAmount).toBe(900);
    expect(component.finalBillAmount).toBe(855);
  });

  it('reuses a selected patient instead of creating a new identity', () => {
    component.selectPatient({ patientId: 'P-00001', firstName: 'Existing', lastName: 'Patient', fullName: 'Existing Patient', mobile: '9999999999' });
    http.expectOne('api/v1/patients/P-00001').flush({
      patientId: 'P-00001', title: 'Mrs', firstName: 'Existing', lastName: 'Patient', fullName: 'Existing Patient',
      gender: 'FEMALE', age: 30, mobile: '9999999999', address: null, aadhaarMasked: 'XXXXXXXX1234',
      email: null, dateOfBirth: null, referredBy: 'SELF', doctorName: null, consultationFee: 0,
      registrationDate: '2026-10-07T10:00:00Z', active: true, createdAt: '2026-10-07T10:00:00Z',
      updatedAt: '2026-10-07T10:00:00Z', version: 0
    });
    expect(component.patientMode).toBe('existing');
    expect(component.selectedPatient?.patientId).toBe('P-00001');
    expect(component.form.value.aadhaarNo).toBe('');
  });
});
