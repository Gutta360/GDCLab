import { Gender } from '../../patients/models/patient.model';

export type ReferralType = 'DOCTOR' | 'LAB' | 'SELF' | 'OTHER';
export type PaymentMode = 'CASH' | 'UPI' | 'CARD' | 'OTHER';
export type RegistrationStatus = 'OPEN' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELLED';
export type TestStatus = 'REGISTERED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type TestDateMode = 'REGISTERED' | 'COMPLETED';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PagedReportResponse<T, S> {
  page: PageResponse<T>;
  summary: S;
}

export interface BranchResponse {
  id: number;
  branchCode: string;
  branchName: string;
  address: string;
  phone: string | null;
  mobile: string | null;
}

export interface ReferralSourceResponse {
  id: number;
  referralCode: string;
  referralType: ReferralType;
  name: string;
  qualification: string | null;
  mobile: string | null;
}

export interface TestCategoryResponse {
  id: number;
  categoryCode: string;
  categoryName: string;
  displayOrder: number;
}

export interface DiagnosticTestResponse {
  testCode: string;
  testName: string;
  categoryCode: string;
  categoryName: string;
  currentPrice: number;
  displayOrder: number;
  active: boolean;
}

export interface PaymentCreateRequest {
  amount: number;
  paymentMode: PaymentMode;
  referenceNo: string | null;
  remarks: string | null;
}

export interface PaymentResponse {
  paymentNo: string;
  amount: number;
  paymentMode: PaymentMode;
  paymentDatetime: string;
  referenceNo: string | null;
  remarks: string | null;
  receivedBy: string | null;
}

export interface RegistrationPatientRequest {
  patientId: string | null;
  updateExisting: boolean;
  title: string | null;
  firstName: string;
  lastName: string | null;
  gender: Gender;
  age: number;
  mobile: string;
  address: string | null;
  email: string | null;
  dateOfBirth: string | null;
  aadhaarNo: string | null;
}

export interface RegistrationTestRequest {
  testCode: string;
  discountPercentage: number;
}

export interface RegistrationCreateRequest {
  patient: RegistrationPatientRequest;
  branchId: number;
  referringDoctorId: number | null;
  referringLabId: number | null;
  registrationDatetime: string | null;
  tests: RegistrationTestRequest[];
  discountPercentage: number;
  initialPayment: PaymentCreateRequest | null;
  remarks: string | null;
}

export interface RegistrationTestResponse {
  testCode: string;
  testName: string;
  categoryCode: string;
  categoryName: string;
  testPrice: number;
  discountPercentage: number;
  discountAmount: number;
  totalAmount: number;
  displayOrder: number;
  status: TestStatus;
  registeredAt: string;
  completedAt: string | null;
  version: number;
}

export interface RegistrationResponse {
  registrationNo: string;
  billNo: string;
  registrationDatetime: string;
  patientId: string;
  patientTitle: string | null;
  patientName: string;
  patientAge: number;
  patientGender: Gender;
  patientMobile: string;
  patientAddress: string | null;
  branchId: number;
  branchCode: string;
  branchName: string;
  referringDoctorId: number | null;
  referringDoctorName: string | null;
  referringDoctorQualification: string | null;
  referringLabId: number | null;
  referringLabName: string | null;
  tests: RegistrationTestResponse[];
  billAmount: number;
  discountPercentage: number;
  discountAmount: number;
  finalBillAmount: number;
  paidAmount: number;
  dueAmount: number;
  status: RegistrationStatus;
  remarks: string | null;
  payments: PaymentResponse[];
  version: number;
}

export interface RegistrationSearchRow {
  registrationNo: string;
  billNo: string;
  registrationDatetime: string;
  patientId: string;
  patientName: string;
  mobile: string;
  testCount: number;
  billAmount: number;
  paidAmount: number;
  dueAmount: number;
  status: RegistrationStatus;
  branchCode: string;
  branchName: string;
}

export interface RegistrationSearchFilters {
  registrationNo?: string;
  billNo?: string;
  patientId?: string;
  patientName?: string;
  mobile?: string;
  branchId?: number;
  fromDate?: string;
  toDate?: string;
  page: number;
  size: number;
}

export interface RegistrationTestSearchRow {
  businessDatetime: string;
  testCode: string;
  testName: string;
  categoryCode: string;
  categoryName: string;
  status: TestStatus;
  registrationNo: string;
  billNo: string;
  patientName: string;
  testAmount: number;
  discountAmount: number;
  finalTestAmount: number;
  branchCode: string;
  branchName: string;
}

export interface TestSearchSummary {
  totalTests: number;
  grossAmount: number;
  testDiscountAmount: number;
  netTestAmount: number;
}

export interface TestSearchFilters {
  testCode?: string;
  testName?: string;
  category?: string;
  registrationNo?: string;
  patientName?: string;
  branchId?: number;
  status?: TestStatus;
  fromDate?: string;
  toDate?: string;
  dateMode: TestDateMode;
  page: number;
  size: number;
}

export interface PaymentSearchRow {
  paymentNo: string;
  paymentDatetime: string;
  registrationNo: string;
  billNo: string;
  patientName: string;
  mobile: string;
  paymentMode: PaymentMode;
  amount: number;
  receivedBy: string | null;
  branchCode: string;
  branchName: string;
}

export interface PaymentSearchSummary {
  paymentCount: number;
  cashTotal: number;
  upiTotal: number;
  cardTotal: number;
  otherTotal: number;
  grandTotal: number;
}

export interface PaymentSearchFilters {
  paymentNo?: string;
  billNo?: string;
  registrationNo?: string;
  patientName?: string;
  mobile?: string;
  paymentMode?: PaymentMode;
  branchId?: number;
  fromDate?: string;
  toDate?: string;
  page: number;
  size: number;
}

export interface BillTestLine {
  sequence: number;
  testName: string;
  testAmount: number;
  discountPercentage: number;
  discountAmount: number;
  totalAmount: number;
}

export interface BillResponse {
  branchName: string;
  branchAddress: string;
  branchPhone: string | null;
  branchMobile: string | null;
  registrationNo: string;
  billNo: string;
  registrationDatetime: string;
  patientTitle: string | null;
  patientName: string;
  patientAge: number;
  patientGender: Gender;
  patientMobile: string;
  patientAddress: string | null;
  referringDoctorName: string | null;
  referringDoctorQualification: string | null;
  referringLabName: string | null;
  tests: BillTestLine[];
  billAmount: number;
  discountPercentage: number;
  discountAmount: number;
  finalBillAmount: number;
  paidAmount: number;
  dueAmount: number;
  preparedBy: string | null;
}
