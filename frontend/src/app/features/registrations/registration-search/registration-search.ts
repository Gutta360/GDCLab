import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { BranchResponse, PageResponse, RegistrationSearchRow } from '../../diagnostics/models/lab.model';
import { BranchService } from '../../diagnostics/services/branch.service';
import { RegistrationService } from '../../diagnostics/services/registration.service';
import { apiErrorMessage } from '../../../shared/utils/api-error';
import { DatePreset, dateRangeFor } from '../../../shared/utils/date-presets';

@Component({
  selector: 'app-registration-search',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './registration-search.html',
  styleUrl: './registration-search.scss'
})
export class RegistrationSearch implements OnInit {
  private readonly fb = inject(FormBuilder);
  loading = false;
  branches: BranchResponse[] = [];
  result: PageResponse<RegistrationSearchRow> = { content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 };
  activePreset: DatePreset = 'TODAY';
  errorMessage = '';

  readonly form = this.fb.group({
    registrationNo: [''], billNo: [''], patientId: [''], patientName: [''], mobile: [''],
    branchId: [''], fromDate: [''], toDate: ['']
  });

  readonly presets: { value: DatePreset; label: string }[] = [
    { value: 'TODAY', label: 'Today' }, { value: 'YESTERDAY', label: 'Yesterday' },
    { value: 'THIS_WEEK', label: 'This Week' }, { value: 'LAST_WEEK', label: 'Last Week' },
    { value: 'THIS_MONTH', label: 'This Month' }, { value: 'LAST_MONTH', label: 'Last Month' },
    { value: 'CUSTOM', label: 'Custom' }
  ];

  constructor(
    private branchService: BranchService,
    private registrationService: RegistrationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.applyPreset('TODAY', false);
    this.loading = true;
    forkJoin({ branches: this.branchService.getActive(), registrations: this.registrationService.search(this.filters(0)) })
      .pipe(finalize(() => { this.loading = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: value => { this.branches = value.branches; this.result = value.registrations; },
        error: error => this.errorMessage = apiErrorMessage(error, 'Could not load registrations.')
      });
  }

  applyPreset(preset: DatePreset, search = true): void {
    this.activePreset = preset;
    const range = dateRangeFor(preset);
    if (range) this.form.patchValue(range);
    if (search) this.search(0);
  }

  customDateChanged(): void {
    this.activePreset = 'CUSTOM';
  }

  search(page = 0): void {
    this.loading = true;
    this.errorMessage = '';
    this.registrationService.search(this.filters(page)).pipe(finalize(() => {
      this.loading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: result => this.result = result,
      error: error => this.errorMessage = apiErrorMessage(error, 'Could not search registrations.')
    });
  }

  reset(): void {
    this.form.reset({ registrationNo: '', billNo: '', patientId: '', patientName: '', mobile: '', branchId: '', fromDate: '', toDate: '' });
    this.applyPreset('TODAY');
  }

  private filters(page: number) {
    const value = this.form.getRawValue();
    return {
      registrationNo: value.registrationNo?.trim() || undefined,
      billNo: value.billNo?.trim() || undefined,
      patientId: value.patientId?.trim() || undefined,
      patientName: value.patientName?.trim() || undefined,
      mobile: value.mobile?.trim() || undefined,
      branchId: value.branchId ? Number(value.branchId) : undefined,
      fromDate: value.fromDate || undefined,
      toDate: value.toDate || undefined,
      page,
      size: 25
    };
  }
}
