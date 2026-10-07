import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PaymentService } from './payment.service';

describe('PaymentService', () => {
  let service: PaymentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(PaymentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('submits a later payment only to the selected registration', () => {
    const body = { amount: 500, paymentMode: 'UPI' as const, referenceNo: 'upi-1', remarks: null };
    service.create('REG-00001', body).subscribe();
    const request = http.expectOne('api/v1/registrations/REG-00001/payments');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush({ paymentNo: 'PAY-00001', ...body, paymentDatetime: '2026-10-07T05:00:00Z', receivedBy: null });
  });
});
