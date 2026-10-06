import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output
} from '@angular/core';

import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { DecimalInputDirective } from '../../../../shared/directives/decimal-input.directive';
import { IntegerInputDirective } from '../../../../shared/directives/integer-input.directive';

@Component({
  selector: 'app-patient-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DecimalInputDirective,
    IntegerInputDirective
  ],
  templateUrl: './patient-form.html',
  styleUrl: './patient-form.scss'
})
export class PatientForm implements OnInit {

  @Input() mode: 'create' | 'edit' = 'create';

  @Input() initialData: any = null;

  @Input() loading = false;

  @Output() formSubmit = new EventEmitter<any>();

  patientForm!: FormGroup;

  readonly genderOptions = [
    { value: 'MALE', label: 'Male' },
    { value: 'FEMALE', label: 'Female' },
    { value: 'OTHER', label: 'Other' }
  ];

  readonly referredByOptions = [
    { value: 'DOCTOR', label: 'Doctor' },
    { value: 'PATIENT', label: 'Patient' },
    { value: 'ONLINE', label: 'Online' },
    { value: 'SELF', label: 'Self' },
    { value: 'OTHER', label: 'Other' }
  ];

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {

    this.patientForm = this.fb.group({

      registrationDate: [
        this.toDateTimeLocalValue(new Date()),
        Validators.required
      ],

      firstName: [
        '',
        [
          Validators.required,
          Validators.minLength(2),
          Validators.maxLength(50),
          Validators.pattern(/^[A-Za-z ]+$/)
        ]
      ],

      lastName: [
        '',
        [
          Validators.maxLength(50),
          Validators.pattern(/^[A-Za-z ]*$/)
        ]
      ],

      gender: [
        '',
        Validators.required
      ],

      age: [
        '',
        [
          Validators.required,
          Validators.min(1),
          Validators.max(120)
        ]
      ],

      mobile: [
        '',
        [
          Validators.required,
          Validators.pattern(/^[0-9]{10}$/)
        ]
      ],

      address: [
        '',
        [
          Validators.required,
          Validators.minLength(2),
          Validators.maxLength(255)
        ]
      ],

      referredBy: [
        '',
        Validators.required
      ],

      doctorName: [''],

      consultationFee: [
        '0.00',
        [
          Validators.required,
          this.consultationFeeValidator
        ]
      ]

    });

    this.patientForm
      .get('referredBy')
      ?.valueChanges
      .subscribe(value => {
        this.updateDoctorNameValidation(value);
      });

    /*
     * Edit mode:
     * populate the same form with existing patient data.
     */
    if (this.initialData) {
      this.populateForm(this.initialData);
    }
  }


  /* =========================
     Convenient Form Access
     ========================= */

  get f() {
    return this.patientForm.controls;
  }


  get isDoctorReferral(): boolean {
    return this.patientForm.get('referredBy')?.value === 'DOCTOR';
  }


  /* =========================
     Create / Update Label
     ========================= */

  get submitLabel(): string {
    return this.mode === 'edit'
      ? 'Update'
      : 'Create';
  }


  /* =========================
     Doctor Validation
     ========================= */

  private updateDoctorNameValidation(referredBy: string): void {

    const doctorName =
      this.patientForm.get('doctorName');

    if (!doctorName) {
      return;
    }

    if (referredBy === 'DOCTOR') {

  doctorName.setValidators([
    Validators.required,
    Validators.minLength(2),
    Validators.maxLength(100)
  ]);

} else {

      doctorName.clearValidators();

      doctorName.setValue(
        '',
        { emitEvent: false }
      );
    }

    doctorName.updateValueAndValidity();
  }


  /* =========================
     Consultation Fee
     ========================= */

  private consultationFeeValidator(
    control: AbstractControl
  ): ValidationErrors | null {

    const value =
      String(control.value ?? '').trim();

    if (!value) {
      return null;
    }

    if (!/^\d+(\.\d{1,2})?$/.test(value)) {
      return {
        invalidFee: true
      };
    }

    const amount = Number(value);

    if (amount < 0) {
      return {
        negativeFee: true
      };
    }

    return null;
  }


  /* =========================
     Input Cleanup
     ========================= */

  normalizeName(controlName: string): void {

    const control =
      this.patientForm.get(controlName);

    if (!control) {
      return;
    }

    let value =
      String(control.value ?? '');

    value = value
      .replace(/[^A-Za-z ]/g, '')
      .replace(/\s+/g, ' ')
      .replace(/^\s+/, '');

    control.setValue(
      value,
      { emitEvent: false }
    );
  }


  normalizeMobile(): void {

    const control =
      this.patientForm.get('mobile');

    if (!control) {
      return;
    }

    const value =
      String(control.value ?? '')
        .replace(/\D/g, '')
        .slice(0, 10);

    control.setValue(
      value,
      { emitEvent: false }
    );
  }


  /* =========================
     Edit Mode Population
     ========================= */

  private populateForm(patient: any): void {

    this.patientForm.patchValue({

      registrationDate:
        patient.registrationDate
          ? this.toDateTimeLocalValue(
              new Date(patient.registrationDate)
            )
          : this.toDateTimeLocalValue(new Date()),

      firstName:
        patient.firstName ?? '',

      lastName:
        patient.lastName ?? '',

      gender:
        patient.gender ?? '',

      age:
        patient.age ?? '',

      mobile:
        patient.mobile ?? '',

      address:
        patient.address ?? '',

      referredBy:
        patient.referredBy ?? '',

      doctorName:
        patient.doctorName ?? '',

      consultationFee:
        patient.consultationFee ?? '0.00'

    });

    this.updateDoctorNameValidation(
      patient.referredBy
    );
  }


  /* =========================
     Submit
     ========================= */

  onSubmit(): void {

    if (this.patientForm.invalid) {

      this.patientForm.markAllAsTouched();
      return;
    }

    const formValue =
      this.patientForm.getRawValue();

    const patientData = {

      firstName:
        formValue.firstName.trim(),

      lastName:
        formValue.lastName?.trim() || null,

      gender:
        formValue.gender,

      age:
        Number(formValue.age),

      mobile:
        formValue.mobile,

      address:
        formValue.address.trim(),

      referredBy:
        formValue.referredBy,

      doctorName:
        formValue.referredBy === 'DOCTOR'
          ? formValue.doctorName.trim()
          : null,

      consultationFee:
        Number(formValue.consultationFee),

      registrationDate:
        new Date(
          formValue.registrationDate
        ).toISOString()

    };

    this.formSubmit.emit(patientData);
  }


  /* =========================
     Date Conversion
     ========================= */

  private toDateTimeLocalValue(
    date: Date
  ): string {

    const pad = (value: number): string =>
      value.toString().padStart(2, '0');

    return (
      `${date.getFullYear()}-` +
      `${pad(date.getMonth() + 1)}-` +
      `${pad(date.getDate())}T` +
      `${pad(date.getHours())}:` +
      `${pad(date.getMinutes())}`
    );
  }

}
