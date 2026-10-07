import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DiagnosticTestResponse, TestCategoryResponse } from '../models/lab.model';

@Injectable({ providedIn: 'root' })
export class DiagnosticTestService {
  private readonly apiUrl = 'api/v1';

  constructor(private http: HttpClient) {}

  getCategories(): Observable<TestCategoryResponse[]> {
    return this.http.get<TestCategoryResponse[]>(`${this.apiUrl}/test-categories`);
  }

  search(query?: string, category?: string, active?: boolean): Observable<DiagnosticTestResponse[]> {
    const params: Record<string, string> = {};
    if (query?.trim()) params['query'] = query.trim();
    if (category) params['category'] = category;
    if (active !== undefined) params['active'] = String(active);
    return this.http.get<DiagnosticTestResponse[]>(`${this.apiUrl}/tests`, { params });
  }

  get(testCode: string): Observable<DiagnosticTestResponse> {
    return this.http.get<DiagnosticTestResponse>(`${this.apiUrl}/tests/${encodeURIComponent(testCode)}`);
  }
}
