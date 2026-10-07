import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { PaymentMode, RegistrationResponse, RegistrationTestResponse, TestStatus } from '../../diagnostics/models/lab.model';
import { PaymentService } from '../../diagnostics/services/payment.service';
import { RegistrationService } from '../../diagnostics/services/registration.service';
import { apiErrorMessage } from '../../../shared/utils/api-error';

@Component({
  selector: 'app-registration-details',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './registration-details.html',
  styleUrl: './registration-details.scss'
})
export class RegistrationDetails implements OnInit {
  private readonly fb = inject(FormBuilder);
  registrationNo = '';
  registration: RegistrationResponse | null = null;
  loading = true;
  savingPayment = false;
  updatingTest = '';
  paymentOpen = false;
  notification = '';
  notificationError = false;
  readonly paymentModes: PaymentMode[] = ['CASH', 'UPI', 'CARD', 'OTHER'];

  readonly paymentForm = this.fb.group({
    amount: ['', [Validators.required, Validators.min(0.01)]],
    paymentMode: ['CASH' as PaymentMode, Validators.required],
    referenceNo: ['', Validators.maxLength(100)],
    remarks: ['', Validators.maxLength(1000)]
  });

  constructor(
    private route: ActivatedRoute,
    private registrationService: RegistrationService,
    private paymentService: PaymentService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.registrationNo = this.route.snapshot.paramMap.get('registrationNo') ?? '';
    this.load();
  }

  load(): void {
    this.loading = true;
    this.registrationService.get(this.registrationNo).pipe(finalize(() => {
      this.loading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: registration => this.registration = registration,
      error: error => this.show(apiErrorMessage(error, 'Could not load registration.'), true)
    });
  }

  nextStatuses(test: RegistrationTestResponse): TestStatus[] {
    if (test.status === 'REGISTERED') return ['IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
    if (test.status === 'IN_PROGRESS') return ['COMPLETED', 'CANCELLED'];
    return [];
  }

  updateStatus(test: RegistrationTestResponse, status: TestStatus): void {
    if (!confirm(`Change ${test.testCode} to ${status.replace('_', ' ')}?`)) return;
    this.updatingTest = test.testCode;
    this.registrationService.updateTestStatus(this.registrationNo, test.testCode, status, test.version)
      .pipe(finalize(() => { this.updatingTest = ''; this.cdr.detectChanges(); }))
      .subscribe({
        next: updated => {
          if (this.registration) {
            this.registration = {
              ...this.registration,
              tests: this.registration.tests.map(item => item.testCode === updated.testCode ? updated : item)
            };
          }
          this.show(`${test.testCode} updated to ${status.replace('_', ' ')}.`, false);
        },
        error: error => {
          this.show(apiErrorMessage(error, 'Could not update test status. Refresh and try again.'), true);
          this.load();
        }
      });
  }

  statusChanged(test: RegistrationTestResponse, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const status = select.value as TestStatus;
    select.value = '';
    if (status) this.updateStatus(test, status);
  }

  openPayment(): void {
    if (!this.registration || this.registration.dueAmount <= 0) return;
    this.paymentForm.reset({ amount: String(this.registration.dueAmount), paymentMode: 'CASH', referenceNo: '', remarks: '' });
    this.paymentOpen = true;
  }

  submitPayment(): void {
    if (!this.registration || this.paymentForm.invalid || this.savingPayment) {
      this.paymentForm.markAllAsTouched();
      return;
    }
    const value = this.paymentForm.getRawValue();
    const amount = Number(value.amount);
    if (amount > this.registration.dueAmount) {
      this.show('Payment cannot exceed the current due amount.', true);
      return;
    }
    this.savingPayment = true;
    this.paymentService.create(this.registrationNo, {
      amount,
      paymentMode: value.paymentMode as PaymentMode,
      referenceNo: value.referenceNo?.trim() || null,
      remarks: value.remarks?.trim() || null
    }).pipe(finalize(() => {
      this.savingPayment = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: payment => {
        this.paymentOpen = false;
        this.show(`Payment ${payment.paymentNo} recorded.`, false);
        this.load();
      },
      error: error => this.show(apiErrorMessage(error, 'Could not record payment.'), true)
    });
  }

  private show(message: string, error: boolean): void {
    this.notification = message;
    this.notificationError = error;
    window.setTimeout(() => { if (this.notification === message) this.notification = ''; this.cdr.detectChanges(); }, 5000);
  }
}
