import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PagedReportResponse,
  PaymentCreateRequest,
  PaymentResponse,
  PaymentSearchFilters,
  PaymentSearchRow,
  PaymentSearchSummary
} from '../models/lab.model';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly apiUrl = 'api/v1';

  constructor(private http: HttpClient) {}

  create(registrationNo: string, request: PaymentCreateRequest): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(
      `${this.apiUrl}/registrations/${encodeURIComponent(registrationNo)}/payments`,
      request
    );
  }

  search(filters: PaymentSearchFilters): Observable<PagedReportResponse<PaymentSearchRow, PaymentSearchSummary>> {
    const params = Object.fromEntries(
      Object.entries(filters)
        .filter(([, value]) => value !== undefined && value !== null && value !== '')
        .map(([key, value]) => [key, String(value)])
    );
    return this.http.get<PagedReportResponse<PaymentSearchRow, PaymentSearchSummary>>(`${this.apiUrl}/payments`, { params });
  }
}
