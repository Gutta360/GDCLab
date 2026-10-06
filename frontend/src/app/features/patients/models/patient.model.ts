export type Gender =
  | 'MALE'
  | 'FEMALE'
  | 'OTHER';


export type ReferredBy =
  | 'DOCTOR'
  | 'PATIENT'
  | 'ONLINE'
  | 'SELF'
  | 'OTHER';


/*
 * Matches:
 * PatientCreateRequest
 * PatientUpdateRequest
 *
 * Both backend DTOs currently have
 * the same request structure.
 */
export interface PatientRequest {

  firstName: string;

  lastName: string | null;

  gender: Gender;

  age: number;

  mobile: string;

  address: string;

  referredBy: ReferredBy;

  doctorName: string | null;

  consultationFee: number;

  registrationDate: string;
}


/*
 * Matches backend PatientResponse.
 */
export interface PatientResponse {

  patientId: string;

  firstName: string;

  lastName: string | null;

  fullName: string;

  gender: Gender;

  age: number;

  mobile: string;

  address: string;

  referredBy: ReferredBy;

  doctorName: string | null;

  consultationFee: number;

  registrationDate: string;

  active: boolean;

  createdAt: string;

  updatedAt: string;

  version: number;
}


/*
 * Lightweight model used by the
 * Patient Details search dropdown.
 */
export interface PatientSummary {

  patientId: string;

  firstName: string;

  lastName: string | null;

  fullName: string;

  mobile?: string | null;
}
