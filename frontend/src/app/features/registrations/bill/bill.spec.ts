import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { Bill } from './bill';

describe('Bill', () => {
  let fixture: ComponentFixture<Bill>;
  let http: HttpTestingController;

  beforeEach(async () => {
    Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', {
      configurable: true,
      value: () => ({ font: '', measureText: () => ({ width: 80 }) })
    });
    await TestBed.configureTestingModule({
      imports: [Bill],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ registrationNo: 'REG-00001' }), queryParamMap: convertToParamMap({}) } } }
      ]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Bill);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('renders persisted bill snapshots without Aadhaar', async () => {
    http.expectOne('api/v1/registrations/REG-00001/bill').flush({
      branchName: 'SRI BALAJI DIAGNOSTICS', branchAddress: 'Main Road', branchPhone: '123', branchMobile: '9999999999',
      registrationNo: 'REG-00001', billNo: 'BILL-00001', registrationDatetime: '2026-10-07T05:00:00Z',
      patientTitle: 'Mrs', patientName: 'Test Patient', patientAge: 30, patientGender: 'FEMALE',
      patientMobile: '9999999999', patientAddress: 'Address', referringDoctorName: 'Dr Test',
      referringDoctorQualification: 'MBBS', referringLabName: null,
      tests: [{ sequence: 1, testName: 'CT Test', testAmount: 1000, discountPercentage: 10, discountAmount: 100, totalAmount: 900 }],
      billAmount: 900, discountPercentage: 0, discountAmount: 0, finalBillAmount: 900, paidAmount: 500, dueAmount: 400, preparedBy: null
    });
    fixture.detectChanges();
    await fixture.whenStable();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('BILL-00001');
    expect(text).toContain('CT Test');
    expect(text).not.toContain('Aadhaar');
  });
});
