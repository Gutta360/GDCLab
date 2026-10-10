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
  });

  async function loadMasters(failTests = false): Promise<void> {
    fixture.detectChanges();
    http.expectOne('api/v1/branches').flush([{ id: 1, branchCode: 'MAIN', branchName: 'Main', address: 'Address', phone: null, mobile: null }]);
    http.expectOne(request => request.url === 'api/v1/referral-sources' && request.params.get('type') === 'DOCTOR').flush([]);
    http.expectOne(request => request.url === 'api/v1/referral-sources' && request.params.get('type') === 'LAB').flush([]);
    const testsRequest = http.expectOne(request => request.url === 'api/v1/tests' && request.params.get('active') === 'true');
    if (failTests) {
      testsRequest.flush({ message: 'Query failed' }, { status: 500, statusText: 'Server Error' });
    } else {
      testsRequest.flush([test]);
    }
    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('renders branches and tests from reference data', async () => {
    await loadMasters();

    const branchOptions = Array.from(fixture.nativeElement.querySelectorAll('#branchId option')) as HTMLOptionElement[];
    expect(branchOptions.map(option => option.textContent?.trim())).toContain('MAIN - Main');
    expect(component.form.value.branchId).toBe('1');
    expect(fixture.nativeElement.querySelector('.test-option')?.textContent).toContain('TST-00001');
    expect(component.categories).toEqual([{ code: 'CT_SCAN', name: 'CT Scan' }]);
  });

  it('keeps successful branch data when the test request fails', async () => {
    await loadMasters(true);

    expect(component.branches).toHaveLength(1);
    expect(component.form.value.branchId).toBe('1');
    expect(component.tests).toEqual([]);
    expect(component.notificationError).toBe(true);
    expect(component.notification).toBe('Query failed');
  });

  it('prevents duplicate test selection and calculates discounts', async () => {
    await loadMasters();
    component.addTest(test);
    component.addTest(test);
    expect(component.selectedTests).toHaveLength(1);

    component.setTestDiscount(component.selectedTests[0], '10');
    component.form.patchValue({ billDiscountPercentage: 5 });
    expect(component.lineDiscount(component.selectedTests[0])).toBe(100);
    expect(component.billAmount).toBe(900);
    expect(component.finalBillAmount).toBe(855);
  });

  it('submits person details directly on the registration', async () => {
    await loadMasters();
    component.form.patchValue({
      patientName: 'Keerthi', mobile: '9999999999', aadhaarNo: '123456789012',
      gender: 'FEMALE', age: '30'
    });
    component.addTest(test);
    component.save();

    const request = http.expectOne('api/v1/registrations');
    expect(request.request.body.patientName).toBe('Keerthi');
    expect(request.request.body.patientMobile).toBe('9999999999');
    expect(request.request.body.patientAadhaar).toBe('123456789012');
    expect(request.request.body.patient).toBeUndefined();
    request.flush({ registrationNo: 'REG-00001' });
  });
});
