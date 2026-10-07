import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ReferralSourceResponse, ReferralType } from '../models/lab.model';

@Injectable({ providedIn: 'root' })
export class ReferralService {
  private readonly apiUrl = 'api/v1/referral-sources';

  constructor(private http: HttpClient) {}

  getActive(type?: ReferralType): Observable<ReferralSourceResponse[]> {
    return this.http.get<ReferralSourceResponse[]>(this.apiUrl, {
      params: type ? { type } : {}
    });
  }
}
