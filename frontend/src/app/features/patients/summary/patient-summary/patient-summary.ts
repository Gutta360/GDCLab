import { CommonModule } from '@angular/common';
import {
  Component,
  OnInit
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { PatientResponse } from '../../models/patient.model';
import { PatientService } from '../../services/patient.service';

@Component({
  selector: 'app-patient-summary',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './patient-summary.html',
  styleUrl: './patient-summary.scss'
})
export class PatientSummary implements OnInit {

  patients: PatientResponse[] = [];
  selectedPatient: PatientResponse | null = null;
  searchTerm = '';
  loading = false;
  errorMessage = '';

  constructor(
    private patientService: PatientService
  ) {}

  ngOnInit(): void {
    this.loadPatients();
  }

  get filteredPatients(): PatientResponse[] {
    const query = this.searchTerm.trim().toLowerCase();

    if (!query) {
      return this.patients;
    }

    return this.patients.filter(patient =>
      patient.patientId.toLowerCase().includes(query)
      || patient.fullName.toLowerCase().includes(query)
      || patient.mobile.includes(query)
    );
  }

  get activeCount(): number {
    return this.patients.filter(patient => patient.active).length;
  }

  get inactiveCount(): number {
    return this.patients.length - this.activeCount;
  }

  selectPatient(patient: PatientResponse): void {
    this.selectedPatient = patient;
  }

  formatDate(value: string): string {
    return new Intl.DateTimeFormat('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    }).format(new Date(value));
  }

  private loadPatients(): void {
    this.loading = true;
    this.errorMessage = '';

    this.patientService
      .getAllPatients()
      .pipe(finalize(() => {
        this.loading = false;
      }))
      .subscribe({
        next: patients => {
          this.patients = patients;
          this.selectedPatient = patients[0] ?? null;
        },
        error: error => {
          this.errorMessage = error?.error?.message ?? 'Could not load patient summary.';
          this.patients = [];
          this.selectedPatient = null;
        }
      });
  }
}
