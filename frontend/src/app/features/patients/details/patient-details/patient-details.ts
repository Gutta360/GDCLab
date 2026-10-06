import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  OnInit
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PatientService } from '../../services/patient.service';
import { PatientResponse } from '../../models/patient.model';

interface PatientSummary {
  patientId: string;
  firstName: string;
  lastName?: string | null;
}

@Component({
  selector: 'app-patient-details',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './patient-details.html',
  styleUrl: './patient-details.scss'
})
export class PatientDetails implements OnInit {

  searchText = '';
  dropdownOpen = false;

  selectedPatientId: string | null = null;

  loadingPatients = false;
  loadingDetails = false;

  patients: PatientSummary[] = [];

  patient: PatientResponse | null = null;

  constructor(
    private patientService: PatientService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.loadPatients();
  }

  /*
   * Load patients for dropdown
   *
   * GET /api/v1/patients
   */
  private loadPatients(): void {

    this.loadingPatients = true;

    this.patientService
      .getAllPatients()
      .subscribe({

        next: patients => {

          this.patients = patients.map(patient => ({
            patientId: patient.patientId,
            firstName: patient.firstName,
            lastName: patient.lastName
          }));

          this.loadingPatients = false;
        },

        error: error => {

          console.error(
            'Failed to load patients',
            error
          );

          this.patients = [];
          this.loadingPatients = false;
        }

      });
  }

  /*
   * Filter dropdown using:
   *
   * Patient ID
   * First Name
   * Last Name
   */
  get filteredPatients(): PatientSummary[] {

    const search =
      this.searchText
        .trim()
        .toLowerCase();

    if (!search) {
      return this.patients;
    }

    return this.patients.filter(patient => {

      const fullName =
        `${patient.firstName} ${patient.lastName ?? ''}`
          .trim()
          .toLowerCase();

      return (
        patient.patientId
          .toLowerCase()
          .includes(search) ||

        fullName.includes(search)
      );
    });
  }

  get selectedPatientLabel(): string {

    if (!this.selectedPatientId) {
      return 'Select patient';
    }

    const selected =
      this.patients.find(
        patient =>
          patient.patientId ===
          this.selectedPatientId
      );

    if (!selected) {
      return this.selectedPatientId;
    }

    return this.patientLabel(selected);
  }

  toggleDropdown(): void {

    this.dropdownOpen =
      !this.dropdownOpen;

    if (this.dropdownOpen) {
      this.searchText = '';
    }
  }

  closeDropdown(): void {
    this.dropdownOpen = false;
  }

  selectPatient(
    selected: PatientSummary
  ): void {

    this.selectedPatientId =
      selected.patientId;

    this.dropdownOpen = false;
    this.searchText = '';

    this.loadPatientDetails(
      selected.patientId
    );
  }

  patientLabel(
    patient: PatientSummary
  ): string {

    const fullName =
      `${patient.firstName} ${patient.lastName ?? ''}`
        .trim();

    return `${patient.patientId}  ${fullName}`;
  }

  /*
   * Load selected patient
   *
   * GET /api/v1/patients/{patientId}
   */
  private loadPatientDetails(
    patientId: string
  ): void {

    this.loadingDetails = true;

    this.patientService
      .getPatient(patientId)
      .subscribe({

        next: patient => {

          this.patient = patient;
          this.loadingDetails = false;

          this.cdr.detectChanges();
        },

        error: error => {

          console.error(
            `Failed to load patient ${patientId}`,
            error
          );

          this.loadingDetails = false;

          this.cdr.detectChanges();
        }

      });
  }

  editPatient(): void {

    if (!this.selectedPatientId) {
      return;
    }

    this.router.navigate([
      '/patients',
      this.selectedPatientId,
      'edit'
    ]);
  }

  formatGender(value: string): string {

    switch (value) {

      case 'MALE':
        return 'Male';

      case 'FEMALE':
        return 'Female';

      case 'OTHER':
        return 'Other';

      default:
        return value || '--';
    }
  }

  formatReferredBy(value: string): string {

    switch (value) {

      case 'DOCTOR':
        return 'Doctor';

      case 'PATIENT':
        return 'Patient';

      case 'ONLINE':
        return 'Online';

      case 'SELF':
        return 'Self';

      case 'OTHER':
        return 'Other';

      default:
        return value || '--';
    }
  }

  formatFee(value: number): string {

    if (value === 0) {
      return 'Free';
    }

    return `₹${value.toFixed(2)}`;
  }
}