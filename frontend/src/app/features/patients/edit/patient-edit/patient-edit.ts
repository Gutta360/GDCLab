import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { PatientForm } from '../../components/patient-form/patient-form';
import { PatientRequest } from '../../models/patient.model';
import { PatientService } from '../../services/patient.service';


@Component({
  selector: 'app-patient-edit',
  standalone: true,
  imports: [
    CommonModule,
    PatientForm
  ],
  templateUrl: './patient-edit.html',
  styleUrl: './patient-edit.scss'
})
export class PatientEdit implements OnInit, OnDestroy {

  patientId = '';

  loading = false;
  loadingPatient = true;

  patient: any = null;
  snackbarMessage = '';
  snackbarType: 'success' | 'error' = 'success';

  private snackbarTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private patientService: PatientService,
    private cdr: ChangeDetectorRef
  ) {}


  ngOnInit(): void {

    this.patientId =
      this.route.snapshot.paramMap.get(
        'patientId'
      ) ?? '';

    this.loadPatient();
  }


  ngOnDestroy(): void {

    if (this.snackbarTimeout) {
      clearTimeout(this.snackbarTimeout);
    }
  }


  private loadPatient(): void {

    this.loadingPatient = true;

    this.patientService
      .getPatient(this.patientId)
      .pipe(finalize(() => {
        this.loadingPatient = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: patient => {
          this.patient = patient;
        },
        error: error => {
          this.patient = null;
          this.showSnackbar(
            error?.error?.message ?? 'Could not load patient.',
            'error'
          );
        }
      });
  }


  onPatientUpdate(
    patientData: PatientRequest
  ): void {

    if (this.loading) {
      return;
    }

    this.loading = true;

    this.patientService
      .updatePatient(
        this.patientId,
        patientData
      )
      .pipe(finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: patient => {
          this.patient = patient;
          this.showSnackbar(
            'Updated Successfully',
            'success'
          );
        },
        error: error => {
          this.showSnackbar(
            error?.error?.message ?? 'Could not update patient.',
            'error'
          );
        }
      });
  }


  close(): void {

    this.router.navigate(
      ['/patients/details'],
      {
        queryParams: {
          patientId: this.patientId
        }
      }
    );
  }


  private showSnackbar(
    message: string,
    type: 'success' | 'error'
  ): void {

    this.snackbarMessage = message;
    this.snackbarType = type;

    if (this.snackbarTimeout) {
      clearTimeout(this.snackbarTimeout);
    }

    this.snackbarTimeout = setTimeout(() => {
      this.snackbarMessage = '';
      this.snackbarTimeout = null;
      this.cdr.detectChanges();
    }, 5000);

    this.cdr.detectChanges();
  }

}
