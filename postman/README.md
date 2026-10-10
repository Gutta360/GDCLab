# GDCLab API Reference

Primary endpoints:

```text
GET    /api/v1/branches
GET    /api/v1/referral-sources?type=DOCTOR
GET    /api/v1/referral-sources?type=LAB
GET    /api/v1/test-categories
GET    /api/v1/tests
POST   /api/v1/registrations
GET    /api/v1/registrations
GET    /api/v1/registrations/{registrationNo}
GET    /api/v1/registrations/{registrationNo}/bill
PUT    /api/v1/registrations/{registrationNo}/tests/{testCode}/status
GET    /api/v1/registration-tests
POST   /api/v1/registrations/{registrationNo}/payments
GET    /api/v1/payments
```

Registration creation accepts person details directly. No separate person-master API exists.
