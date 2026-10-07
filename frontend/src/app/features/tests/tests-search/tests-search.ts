import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin, switchMap } from 'rxjs';
import {
  BranchResponse,
  PagedReportResponse,
  RegistrationTestSearchRow,
  TestCategoryResponse,
  TestDateMode,
  TestSearchSummary,
  TestStatus
} from '../../diagnostics/models/lab.model';
import { BranchService } from '../../diagnostics/services/branch.service';
import { DiagnosticTestService } from '../../diagnostics/services/diagnostic-test.service';
import { RegistrationService } from '../../diagnostics/services/registration.service';
import { apiErrorMessage } from '../../../shared/utils/api-error';
import { DatePreset, dateRangeFor } from '../../../shared/utils/date-presets';

@Component({
  selector: 'app-tests-search',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './tests-search.html',
  styleUrl: './tests-search.scss'
})
export class TestsSearch implements OnInit {
  private readonly fb = inject(FormBuilder);
  loading = false;
  updating = '';
  branches: BranchResponse[] = [];
  categories: TestCategoryResponse[] = [];
  result: PagedReportResponse<RegistrationTestSearchRow, TestSearchSummary> = {
    page: { content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 },
    summary: { totalTests: 0, grossAmount: 0, testDiscountAmount: 0, netTestAmount: 0 }
  };
  activePreset: DatePreset = 'TODAY';
  message = '';
  messageError = false;

  readonly statuses: TestStatus[] = ['REGISTERED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
  readonly form = this.fb.group({
    testCode: [''], testName: [''], category: [''], registrationNo: [''], patientName: [''],
    branchId: [''], status: [''], fromDate: [''], toDate: [''], dateMode: ['REGISTERED' as TestDateMode]
  });
  readonly presets: { value: DatePreset; label: string }[] = [
    { value: 'TODAY', label: 'Today' }, { value: 'YESTERDAY', label: 'Yesterday' },
    { value: 'THIS_WEEK', label: 'This Week' }, { value: 'LAST_WEEK', label: 'Last Week' },
    { value: 'THIS_MONTH', label: 'This Month' }, { value: 'LAST_MONTH', label: 'Last Month' },
    { value: 'CUSTOM', label: 'Custom' }
  ];

  constructor(
    private branchService: BranchService,
    private testService: DiagnosticTestService,
    private registrationService: RegistrationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.applyPreset('TODAY', false);
    this.loading = true;
    forkJoin({
      branches: this.branchService.getActive(), categories: this.testService.getCategories(),
      tests: this.registrationService.searchTests(this.filters(0))
    }).pipe(finalize(() => { this.loading = false; this.cdr.detectChanges(); })).subscribe({
      next: value => { this.branches = value.branches; this.categories = value.categories; this.result = value.tests; },
      error: error => this.show(apiErrorMessage(error, 'Could not load test report.'), true)
    });
  }

  setMode(mode: TestDateMode): void {
    this.form.patchValue({ dateMode: mode });
    this.search(0);
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
    this.registrationService.searchTests(this.filters(page)).pipe(finalize(() => {
      this.loading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: result => this.result = result,
      error: error => this.show(apiErrorMessage(error, 'Could not search tests.'), true)
    });
  }

  nextStatuses(row: RegistrationTestSearchRow): TestStatus[] {
    if (row.status === 'REGISTERED') return ['IN_PROGRESS', 'COMPLETED', 'CANCELLED'];
    if (row.status === 'IN_PROGRESS') return ['COMPLETED', 'CANCELLED'];
    return [];
  }

  updateStatus(row: RegistrationTestSearchRow, status: TestStatus): void {
    if (!status || !confirm(`Change ${row.testCode} to ${status.replace('_', ' ')}?`)) return;
    this.updating = `${row.registrationNo}:${row.testCode}`;
    this.registrationService.get(row.registrationNo).pipe(
      switchMap(registration => {
        const test = registration.tests.find(item => item.testCode === row.testCode);
        if (!test) throw new Error('Registration test no longer exists.');
        return this.registrationService.updateTestStatus(row.registrationNo, row.testCode, status, test.version);
      }),
      finalize(() => { this.updating = ''; this.cdr.detectChanges(); })
    ).subscribe({
      next: () => { this.show(`${row.testCode} updated.`, false); this.search(this.result.page.page); },
      error: error => this.show(apiErrorMessage(error, 'Status changed elsewhere. Refresh and try again.'), true)
    });
  }

  statusChanged(row: RegistrationTestSearchRow, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const status = select.value as TestStatus;
    select.value = '';
    if (status) this.updateStatus(row, status);
  }

  reset(): void {
    this.form.reset({ testCode: '', testName: '', category: '', registrationNo: '', patientName: '', branchId: '', status: '', fromDate: '', toDate: '', dateMode: 'REGISTERED' });
    this.applyPreset('TODAY');
  }

  private filters(page: number) {
    const value = this.form.getRawValue();
    return {
      testCode: value.testCode?.trim() || undefined,
      testName: value.testName?.trim() || undefined,
      category: value.category || undefined,
      registrationNo: value.registrationNo?.trim() || undefined,
      patientName: value.patientName?.trim() || undefined,
      branchId: value.branchId ? Number(value.branchId) : undefined,
      status: value.status ? value.status as TestStatus : undefined,
      fromDate: value.fromDate || undefined,
      toDate: value.toDate || undefined,
      dateMode: value.dateMode as TestDateMode,
      page,
      size: 25
    };
  }

  private show(message: string, error: boolean): void {
    this.message = message;
    this.messageError = error;
    window.setTimeout(() => { if (this.message === message) this.message = ''; this.cdr.detectChanges(); }, 5000);
  }
}
