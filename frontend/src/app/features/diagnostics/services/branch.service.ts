import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BranchResponse } from '../models/lab.model';

@Injectable({ providedIn: 'root' })
export class BranchService {
  private readonly apiUrl = 'api/v1/branches';

  constructor(private http: HttpClient) {}

  getActive(): Observable<BranchResponse[]> {
    return this.http.get<BranchResponse[]>(this.apiUrl);
  }
}
