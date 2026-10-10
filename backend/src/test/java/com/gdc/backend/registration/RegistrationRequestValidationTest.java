package com.gdc.backend.registration;

import com.gdc.backend.registration.dto.RegistrationCreateRequest;
import com.gdc.backend.registration.dto.RegistrationTestRequest;
import com.gdc.backend.registration.entity.Gender;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationRequestValidationTest {
    private final jakarta.validation.Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test void acceptsNewbornOptionalAddressAndValidAadhaar() {
        assertThat(validator.validate(request((short) 0, "012345678901", LocalDate.of(2026, 10, 6)))).isEmpty();
    }

    @Test void rejectsInvalidAadhaarFutureDobAndMobile() {
        var invalid = new RegistrationCreateRequest("Baby", "Test Person", "999999999",
                "1234ABCD9012", Gender.FEMALE, "baby@example.com", LocalDate.now().plusDays(1),
                (short) 1, null, 1L, null, null, null,
                List.of(new RegistrationTestRequest("TST-00001", BigDecimal.ZERO)), BigDecimal.ZERO, null, null);

        assertThat(validator.validate(invalid)).extracting(v -> v.getPropertyPath().toString())
                .contains("patientAadhaar", "patientDateOfBirth", "patientMobile");
    }

    @Test void doesNotImposePersonIdentityUniquenessOnTheRequest() {
        var firstVisit = request((short) 20, null, null);
        var laterVisit = request((short) 20, null, null);

        assertThat(validator.validate(firstVisit)).isEmpty();
        assertThat(validator.validate(laterVisit)).isEmpty();
    }

    private RegistrationCreateRequest request(short age, String aadhaar, LocalDate dob) {
        return new RegistrationCreateRequest("Mrs", "Test Person", "9999999999", aadhaar,
                Gender.FEMALE, "person@example.com", dob, age, null, 1L, null, null, null,
                List.of(new RegistrationTestRequest("TST-00001", BigDecimal.ZERO)), BigDecimal.ZERO, null, null);
    }
}
