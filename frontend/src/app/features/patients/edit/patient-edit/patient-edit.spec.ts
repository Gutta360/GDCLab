import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { PatientEdit } from './patient-edit';

describe('PatientEdit', () => {
  let component: PatientEdit;
  let fixture: ComponentFixture<PatientEdit>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientEdit],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({
                patientId: 'PAT-001'
              })
            }
          }
        }
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PatientEdit);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const patientRequest = httpMock.expectOne('/api/v1/patients/PAT-001');
    patientRequest.flush({
      patientId: 'PAT-001',
      firstName: 'Test',
      lastName: 'Patient',
      fullName: 'Test Patient',
      gender: 'MALE',
      age: 30,
      mobile: '9999988887',
      address: 'Hyderabad',
      referredBy: 'SELF',
      doctorName: null,
      consultationFee: 500,
      registrationDate: '2026-09-26T11:50:00Z',
      active: true,
      createdAt: '2026-09-26T11:50:00Z',
      updatedAt: '2026-09-26T11:50:00Z',
      version: 1
    });

    await fixture.whenStable();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
