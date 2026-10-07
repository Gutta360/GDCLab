import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RegistrationService } from './registration.service';

describe('RegistrationService', () => {
  let service: RegistrationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(RegistrationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('maps completed test reporting to the backend dateMode parameter', () => {
    service.searchTests({ dateMode: 'COMPLETED', fromDate: '2026-10-01', toDate: '2026-10-07', page: 0, size: 25 }).subscribe();
    const request = http.expectOne(value => value.url === 'api/v1/registration-tests');
    expect(request.request.params.get('dateMode')).toBe('COMPLETED');
    expect(request.request.params.get('fromDate')).toBe('2026-10-01');
    request.flush({ page: { content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 }, summary: { totalTests: 0, grossAmount: 0, testDiscountAmount: 0, netTestAmount: 0 } });
  });

  it('sends expectedVersion with a status transition', () => {
    service.updateTestStatus('REG-00001', 'TST-00001', 'COMPLETED', 4).subscribe();
    const request = http.expectOne('api/v1/registrations/REG-00001/tests/TST-00001/status');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ status: 'COMPLETED', expectedVersion: 4 });
    request.flush({});
  });
});
