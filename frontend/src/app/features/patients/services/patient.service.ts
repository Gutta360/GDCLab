import { Injectable } from '@angular/core';

import {
  HttpClient
} from '@angular/common/http';

import {
  Observable
} from 'rxjs';

import { PatientResponse, PatientSummary } from '../models/patient.model';


@Injectable({
  providedIn: 'root'
})
export class PatientService {

  /*
   * Keep this relative.
   *
   * Development:
   * Angular proxy can forward /api → Spring Boot.
   *
   * Production:
   * Nginx can forward /gdclab/api → Spring Boot :8090
   * when the frontend is built with --base-href /gdclab/.
   *
   * Therefore Angular does not need to know
   * the backend host or port.
   */
  private readonly apiUrl =
    'api/v1/patients';


  constructor(
    private http: HttpClient
  ) {}


  searchActivePatients(
    query: string
  ): Observable<PatientSummary[]> {

    return this.http.get<PatientSummary[]>(
      `${this.apiUrl}/search`,
      {
        params: {
          query
        }
      }
    );
  }


  /*
   * GET /api/v1/patients/{patientId}
   */
  getPatient(
    patientId: string
  ): Observable<PatientResponse> {

    return this.http.get<PatientResponse>(
      `${this.apiUrl}/${patientId}`
    );
  }
}
