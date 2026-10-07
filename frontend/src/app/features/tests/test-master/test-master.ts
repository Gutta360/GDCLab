import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { DiagnosticTestResponse, TestCategoryResponse } from '../../diagnostics/models/lab.model';
import { DiagnosticTestService } from '../../diagnostics/services/diagnostic-test.service';
import { apiErrorMessage } from '../../../shared/utils/api-error';

@Component({
  selector: 'app-test-master',
  imports: [CommonModule, FormsModule],
  templateUrl: './test-master.html',
  styleUrl: './test-master.scss'
})
export class TestMaster implements OnInit {
  loading = true;
  query = '';
  category = '';
  activeFilter: '' | 'true' | 'false' = '';
  tests: DiagnosticTestResponse[] = [];
  categories: TestCategoryResponse[] = [];
  errorMessage = '';

  constructor(private service: DiagnosticTestService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    forkJoin({ tests: this.service.search(), categories: this.service.getCategories() })
      .pipe(finalize(() => { this.loading = false; this.cdr.detectChanges(); }))
      .subscribe({
        next: result => { this.tests = result.tests; this.categories = result.categories; },
        error: error => this.errorMessage = apiErrorMessage(error, 'Could not load the test master.')
      });
  }

  get filtered(): DiagnosticTestResponse[] {
    const query = this.query.trim().toLowerCase();
    return this.tests.filter(test =>
      (!query || test.testCode.toLowerCase().includes(query) || test.testName.toLowerCase().includes(query)) &&
      (!this.category || test.categoryCode === this.category) &&
      (!this.activeFilter || test.active === (this.activeFilter === 'true'))
    );
  }

  count(categoryCode: string): number {
    return this.tests.filter(test => test.categoryCode === categoryCode).length;
  }
}
