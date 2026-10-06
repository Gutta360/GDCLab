import {
  ChangeDetectorRef,
  Component,
  OnDestroy
} from '@angular/core';
import { finalize } from 'rxjs';
import { PatientForm } from '../../components/patient-form/patient-form';
import { PatientRequest } from '../../models/patient.model';
import { PatientService } from '../../services/patient.service';

@Component({
  selector: 'app-patient-registration',
  standalone: true,
  imports: [
    PatientForm
  ],
  templateUrl: './patient-registration.html',
  styleUrl: './patient-registration.scss'
})
export class PatientRegistration implements OnDestroy {

  loading = false;
  snackbarMessage = '';
  snackbarType: 'success' | 'error' = 'success';

  private snackbarTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private patientService: PatientService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnDestroy(): void {

    if (this.snackbarTimeout) {
      clearTimeout(this.snackbarTimeout);
    }
  }

  onPatientCreate(patientData: PatientRequest): void {

    if (this.loading) {
      return;
    }

    this.loading = true;

    this.patientService
      .createPatient(patientData)
      .pipe(finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: () => {
          this.showSnackbar(
            'Registered Successfully',
            'success'
          );
        },
        error: error => {
          this.showSnackbar(
            error?.error?.message ?? 'Could not register patient.',
            'error'
          );
        }
      });
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
