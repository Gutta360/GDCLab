import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  BillResponse,
  PageResponse,
  PagedReportResponse,
  RegistrationCreateRequest,
  RegistrationResponse,
  RegistrationSearchFilters,
  RegistrationSearchRow,
  RegistrationTestResponse,
  RegistrationTestSearchRow,
  TestSearchFilters,
  TestSearchSummary,
  TestStatus
} from '../models/lab.model';

@Injectable({ providedIn: 'root' })
export class RegistrationService {
  private readonly apiUrl = 'api/v1';

  constructor(private http: HttpClient) {}

  create(request: RegistrationCreateRequest): Observable<RegistrationResponse> {
    return this.http.post<RegistrationResponse>(`${this.apiUrl}/registrations`, request);
  }

  get(registrationNo: string): Observable<RegistrationResponse> {
    return this.http.get<RegistrationResponse>(`${this.apiUrl}/registrations/${encodeURIComponent(registrationNo)}`);
  }

  getBill(registrationNo: string): Observable<BillResponse> {
    return this.http.get<BillResponse>(`${this.apiUrl}/registrations/${encodeURIComponent(registrationNo)}/bill`);
  }

  search(filters: RegistrationSearchFilters): Observable<PageResponse<RegistrationSearchRow>> {
    return this.http.get<PageResponse<RegistrationSearchRow>>(`${this.apiUrl}/registrations`, {
      params: this.params(filters)
    });
  }

  searchTests(filters: TestSearchFilters): Observable<PagedReportResponse<RegistrationTestSearchRow, TestSearchSummary>> {
    return this.http.get<PagedReportResponse<RegistrationTestSearchRow, TestSearchSummary>>(`${this.apiUrl}/registration-tests`, {
      params: this.params(filters)
    });
  }

  updateTestStatus(registrationNo: string, testCode: string, status: TestStatus, expectedVersion: number): Observable<RegistrationTestResponse> {
    return this.http.put<RegistrationTestResponse>(
      `${this.apiUrl}/registrations/${encodeURIComponent(registrationNo)}/tests/${encodeURIComponent(testCode)}/status`,
      { status, expectedVersion }
    );
  }

  private params(values: object): Record<string, string> {
    return Object.fromEntries(
      Object.entries(values)
        .filter(([, value]) => value !== undefined && value !== null && value !== '')
        .map(([key, value]) => [key, String(value)])
    );
  }
}
