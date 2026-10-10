import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable, catchError, finalize, forkJoin, of } from 'rxjs';
import {
  BranchResponse,
  DiagnosticTestResponse,
  PaymentMode,
  ReferralSourceResponse,
  RegistrationCreateRequest,
  RegistrationResponse
} from '../../diagnostics/models/lab.model';
import { BranchService } from '../../diagnostics/services/branch.service';
import { DiagnosticTestService } from '../../diagnostics/services/diagnostic-test.service';
import { ReferralService } from '../../diagnostics/services/referral.service';
import { RegistrationService } from '../../diagnostics/services/registration.service';
import { apiErrorMessage } from '../../../shared/utils/api-error';
import { localDate, localDateTime } from '../../../shared/utils/date-presets';

interface SelectedTest {
  master: DiagnosticTestResponse;
  discountPercentage: number;
}

@Component({
  selector: 'app-new-registration',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './new-registration.html',
  styleUrl: './new-registration.scss'
})
export class NewRegistration implements OnInit {
  private readonly fb = inject(FormBuilder);
  loadingMasters = true;
  submitting = false;
  testQuery = '';
  categoryFilter = '';
  branches: BranchResponse[] = [];
  doctors: ReferralSourceResponse[] = [];
  labs: ReferralSourceResponse[] = [];
  tests: DiagnosticTestResponse[] = [];
  selectedTests: SelectedTest[] = [];
  created: RegistrationResponse | null = null;
  notification = '';
  notificationError = false;

  readonly paymentModes: PaymentMode[] = ['CASH', 'UPI', 'CARD', 'OTHER'];
  readonly today = localDate(new Date());

  readonly form = this.fb.group({
    registrationDatetime: [localDateTime(), Validators.required],
    branchId: ['', Validators.required],
    referringDoctorId: [''],
    referringLabId: [''],
    title: [''],
    patientName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(101), Validators.pattern(/^[A-Za-z ]+$/)]],
    mobile: ['', [Validators.required, Validators.pattern(/^\d{10}$/)]],
    aadhaarNo: ['', Validators.pattern(/^\d{12}$/)],
    gender: ['', Validators.required],
    email: ['', [Validators.email, Validators.maxLength(254)]],
    dateOfBirth: [''],
    age: ['', [Validators.required, Validators.min(0), Validators.max(120)]],
    address: ['', Validators.maxLength(255)],
    billDiscountPercentage: [0, [Validators.min(0), Validators.max(100)]],
    initialPaymentAmount: [0, Validators.min(0)],
    paymentMode: ['CASH' as PaymentMode],
    referenceNo: ['', Validators.maxLength(100)],
    remarks: ['', Validators.maxLength(1000)]
  });

  constructor(
    private branchService: BranchService,
    private referralService: ReferralService,
    private testService: DiagnosticTestService,
    private registrationService: RegistrationService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    forkJoin({
      branches: this.referenceData(this.branchService.getActive(), 'branches'),
      doctors: this.referenceData(this.referralService.getActive('DOCTOR'), 'referring doctors'),
      labs: this.referenceData(this.referralService.getActive('LAB'), 'referring labs'),
      tests: this.referenceData(this.testService.search(undefined, undefined, true), 'diagnostic tests')
    }).pipe(finalize(() => {
      this.loadingMasters = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: data => {
        this.branches = data.branches;
        this.doctors = data.doctors;
        this.labs = data.labs;
        this.tests = data.tests;
        if (this.branches.length === 1) this.form.patchValue({ branchId: String(this.branches[0].id) });
      }
    });
  }

  private referenceData<T>(source: Observable<T[]>, label: string): Observable<T[]> {
    return source.pipe(catchError(error => {
      this.show(apiErrorMessage(error, `Could not load ${label}.`), true);
      return of([]);
    }));
  }

  get categories(): { code: string; name: string }[] {
    const values = new Map<string, string>();
    this.tests.forEach(test => values.set(test.categoryCode, test.categoryName));
    return Array.from(values, ([code, name]) => ({ code, name }));
  }

  get filteredTests(): DiagnosticTestResponse[] {
    const query = this.testQuery.trim().toLowerCase();
    return this.tests.filter(test =>
      (!this.categoryFilter || test.categoryCode === this.categoryFilter) &&
      (!query || test.testCode.toLowerCase().includes(query) || test.testName.toLowerCase().includes(query)) &&
      !this.selectedTests.some(selected => selected.master.testCode === test.testCode)
    );
  }

  get billAmount(): number {
    return this.round(this.selectedTests.reduce((sum, line) => sum + this.lineTotal(line), 0));
  }

  get billDiscountAmount(): number {
    return this.round(this.billAmount * this.number(this.form.value.billDiscountPercentage) / 100);
  }

  get finalBillAmount(): number {
    return this.round(this.billAmount - this.billDiscountAmount);
  }

  get previewPaidAmount(): number {
    return Math.min(this.number(this.form.value.initialPaymentAmount), this.finalBillAmount);
  }

  get previewDueAmount(): number {
    return this.round(Math.max(0, this.finalBillAmount - this.previewPaidAmount));
  }

  normalizeDigits(controlName: 'mobile' | 'aadhaarNo', length: number): void {
    const control = this.form.controls[controlName];
    control.setValue(String(control.value ?? '').replace(/\D/g, '').slice(0, length));
  }

  dobChanged(): void {
    const value = this.form.value.dateOfBirth;
    if (!value) return;
    const birth = new Date(`${value}T00:00:00`);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const month = today.getMonth() - birth.getMonth();
    if (month < 0 || (month === 0 && today.getDate() < birth.getDate())) age--;
    if (age >= 0 && age <= 120) this.form.patchValue({ age: String(age) });
  }

  addTest(test: DiagnosticTestResponse): void {
    if (this.selectedTests.some(line => line.master.testCode === test.testCode)) {
      this.show('This test is already selected.', true);
      return;
    }
    this.selectedTests = [...this.selectedTests, { master: test, discountPercentage: 0 }];
  }

  removeTest(testCode: string): void {
    this.selectedTests = this.selectedTests.filter(line => line.master.testCode !== testCode);
  }

  setTestDiscount(line: SelectedTest, rawValue: string): void {
    line.discountPercentage = Math.min(100, Math.max(0, this.number(rawValue)));
    this.selectedTests = [...this.selectedTests];
  }

  discountChanged(line: SelectedTest, event: Event): void {
    this.setTestDiscount(line, (event.target as HTMLInputElement).value);
  }

  lineDiscount(line: SelectedTest): number {
    return this.round(line.master.currentPrice * line.discountPercentage / 100);
  }

  lineTotal(line: SelectedTest): number {
    return this.round(line.master.currentPrice - this.lineDiscount(line));
  }

  save(printAfter = false): void {
    if (this.submitting) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.show('Please correct the highlighted registration fields.', true);
      return;
    }
    if (!this.selectedTests.length) {
      this.show('Select at least one diagnostic test.', true);
      return;
    }
    if (this.form.value.dateOfBirth && this.form.value.dateOfBirth > this.today) {
      this.show('Date of birth cannot be in the future.', true);
      return;
    }
    const initialAmount = this.number(this.form.value.initialPaymentAmount);
    if (initialAmount > this.finalBillAmount) {
      this.show('Initial payment cannot exceed the preview amount due.', true);
      return;
    }

    this.submitting = true;
    this.registrationService.create(this.request(initialAmount)).pipe(finalize(() => {
      this.submitting = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: response => {
        this.created = response;
        this.show(`Registration ${response.registrationNo} saved successfully.`, false);
        if (printAfter) this.router.navigate(['/registrations', response.registrationNo, 'bill'], { queryParams: { print: 1 } });
      },
      error: error => this.show(apiErrorMessage(error, 'Could not save registration.'), true)
    });
  }

  private request(initialAmount: number): RegistrationCreateRequest {
    const value = this.form.getRawValue();
    return {
      patientTitle: value.title?.trim() || null,
      patientName: value.patientName?.trim() || '',
      patientMobile: value.mobile || '',
      patientAadhaar: value.aadhaarNo || null,
      patientGender: value.gender as RegistrationCreateRequest['patientGender'],
      patientEmail: value.email?.trim() || null,
      patientDateOfBirth: value.dateOfBirth || null,
      patientAge: this.number(value.age),
      patientAddress: value.address?.trim() || null,
      branchId: Number(value.branchId),
      referringDoctorId: value.referringDoctorId ? Number(value.referringDoctorId) : null,
      referringLabId: value.referringLabId ? Number(value.referringLabId) : null,
      registrationDatetime: value.registrationDatetime ? new Date(value.registrationDatetime).toISOString() : null,
      tests: this.selectedTests.map(line => ({ testCode: line.master.testCode, discountPercentage: line.discountPercentage })),
      discountPercentage: this.number(value.billDiscountPercentage),
      initialPayment: initialAmount > 0 ? {
        amount: initialAmount,
        paymentMode: value.paymentMode as PaymentMode,
        referenceNo: value.referenceNo?.trim() || null,
        remarks: null
      } : null,
      remarks: value.remarks?.trim() || null
    };
  }

  private number(value: unknown): number {
    const result = Number(value ?? 0);
    return Number.isFinite(result) ? result : 0;
  }

  private round(value: number): number {
    return Math.round((value + Number.EPSILON) * 100) / 100;
  }

  private show(message: string, error: boolean): void {
    this.notification = message;
    this.notificationError = error;
    window.setTimeout(() => {
      if (this.notification === message) this.notification = '';
      this.cdr.detectChanges();
    }, 5000);
  }
}
