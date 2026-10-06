import { Injectable } from '@angular/core';

import {
  HttpClient
} from '@angular/common/http';

import {
  Observable
} from 'rxjs';

import {
  PatientRequest,
  PatientResponse,
  PatientSummary
} from '../models/patient.model';


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


  /*
   * GET /api/v1/patients
   */
  getAllPatients():
    Observable<PatientResponse[]> {

    return this.http.get<PatientResponse[]>(
      this.apiUrl
    );
  }


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


  /*
   * POST /api/v1/patients
   */
  createPatient(
    request: PatientRequest
  ): Observable<PatientResponse> {

    return this.http.post<PatientResponse>(
      this.apiUrl,
      request
    );
  }


  /*
   * PUT /api/v1/patients/{patientId}
   */
  updatePatient(
    patientId: string,
    request: PatientRequest
  ): Observable<PatientResponse> {

    return this.http.put<PatientResponse>(
      `${this.apiUrl}/${patientId}`,
      request
    );
  }


  /*
   * DELETE /api/v1/patients/{patientId}
   *
   * We won't use this yet.
   * Keeping it here completes the Patient API service.
   */
  deletePatient(
    patientId: string
  ): Observable<void> {

    return this.http.delete<void>(
      `${this.apiUrl}/${patientId}`
    );
  }

}
