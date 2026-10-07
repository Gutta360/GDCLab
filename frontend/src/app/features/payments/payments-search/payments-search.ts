import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import {
  BranchResponse,
  PagedReportResponse,
  PaymentMode,
  PaymentSearchRow,
  PaymentSearchSummary
} from '../../diagnostics/models/lab.model';
import { BranchService } from '../../diagnostics/services/branch.service';
import { PaymentService } from '../../diagnostics/services/payment.service';
import { apiErrorMessage } from '../../../shared/utils/api-error';
import { DatePreset, dateRangeFor } from '../../../shared/utils/date-presets';

@Component({
  selector: 'app-payments-search',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './payments-search.html',
  styleUrl: './payments-search.scss'
})
export class PaymentsSearch implements OnInit {
  private readonly fb = inject(FormBuilder);
  loading = false;
  branches: BranchResponse[] = [];
  activePreset: DatePreset = 'TODAY';
  errorMessage = '';
  result: PagedReportResponse<PaymentSearchRow, PaymentSearchSummary> = {
    page: { content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 },
    summary: { paymentCount: 0, cashTotal: 0, upiTotal: 0, cardTotal: 0, otherTotal: 0, grandTotal: 0 }
  };
  readonly modes: PaymentMode[] = ['CASH', 'UPI', 'CARD', 'OTHER'];
  readonly form = this.fb.group({
    paymentNo: [''], billNo: [''], registrationNo: [''], patientName: [''], mobile: [''],
    paymentMode: [''], branchId: [''], fromDate: [''], toDate: ['']
  });
  readonly presets: { value: DatePreset; label: string }[] = [
    { value: 'TODAY', label: 'Today' }, { value: 'YESTERDAY', label: 'Yesterday' },
    { value: 'THIS_WEEK', label: 'This Week' }, { value: 'LAST_WEEK', label: 'Last Week' },
    { value: 'THIS_MONTH', label: 'This Month' }, { value: 'LAST_MONTH', label: 'Last Month' },
    { value: 'CUSTOM', label: 'Custom' }
  ];

  constructor(
    private branchService: BranchService,
    private paymentService: PaymentService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.applyPreset('TODAY', false);
    this.loading = true;
    forkJoin({ branches: this.branchService.getActive(), payments: this.paymentService.search(this.filters(0)) })
      .pipe(finalize(() => { this.loading = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: value => { this.branches = value.branches; this.result = value.payments; },
        error: error => this.errorMessage = apiErrorMessage(error, 'Could not load payments.')
      });
  }

  applyPreset(preset: DatePreset, search = true): void {
    this.activePreset = preset;
    const range = dateRangeFor(preset);
    if (range) this.form.patchValue(range);
    if (search) this.search(0);
  }

  customDateChanged(): void { this.activePreset = 'CUSTOM'; }

  search(page = 0): void {
    this.loading = true;
    this.errorMessage = '';
    this.paymentService.search(this.filters(page)).pipe(finalize(() => {
      this.loading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: result => this.result = result,
      error: error => this.errorMessage = apiErrorMessage(error, 'Could not search payments.')
    });
  }

  reset(): void {
    this.form.reset({ paymentNo: '', billNo: '', registrationNo: '', patientName: '', mobile: '', paymentMode: '', branchId: '', fromDate: '', toDate: '' });
    this.applyPreset('TODAY');
  }

  private filters(page: number) {
    const value = this.form.getRawValue();
    return {
      paymentNo: value.paymentNo?.trim() || undefined,
      billNo: value.billNo?.trim() || undefined,
      registrationNo: value.registrationNo?.trim() || undefined,
      patientName: value.patientName?.trim() || undefined,
      mobile: value.mobile?.trim() || undefined,
      paymentMode: value.paymentMode ? value.paymentMode as PaymentMode : undefined,
      branchId: value.branchId ? Number(value.branchId) : undefined,
      fromDate: value.fromDate || undefined,
      toDate: value.toDate || undefined,
      page,
      size: 25
    };
  }
}
