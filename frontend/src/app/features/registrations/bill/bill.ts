import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import JsBarcode from 'jsbarcode';
import { BillResponse } from '../../diagnostics/models/lab.model';
import { RegistrationService } from '../../diagnostics/services/registration.service';
import { apiErrorMessage } from '../../../shared/utils/api-error';

@Component({
  selector: 'app-bill',
  imports: [CommonModule, RouterLink],
  templateUrl: './bill.html',
  styleUrl: './bill.scss'
})
export class Bill implements OnInit {
  @ViewChild('barcode') barcode?: ElementRef<SVGSVGElement>;
  registrationNo = '';
  bill: BillResponse | null = null;
  loading = true;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private service: RegistrationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.registrationNo = this.route.snapshot.paramMap.get('registrationNo') ?? '';
    const printAfter = this.route.snapshot.queryParamMap.get('print') === '1';
    this.service.getBill(this.registrationNo).pipe(finalize(() => {
      this.loading = false;
      this.cdr.detectChanges();
    })).subscribe({
      next: bill => {
        this.bill = bill;
        window.setTimeout(() => {
          this.renderBarcode();
          if (printAfter) window.print();
        });
      },
      error: error => this.errorMessage = apiErrorMessage(error, 'Could not load bill.')
    });
  }

  print(): void {
    this.renderBarcode();
    window.print();
  }

  private renderBarcode(): void {
    if (!this.bill || !this.barcode) return;
    JsBarcode(this.barcode.nativeElement, this.bill.billNo, {
      format: 'CODE128', displayValue: true, height: 42, width: 1.5, margin: 0,
      font: 'Arial', fontSize: 12, textMargin: 3, background: '#ffffff', lineColor: '#000000'
    });
  }
}
