package com.gdc.backend.patient;

import com.gdc.backend.patient.entity.*;
import com.gdc.backend.registration.dto.RegistrationPatientRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

class PatientRequestValidationTest {
    private final jakarta.validation.Validator validator=Validation.buildDefaultValidatorFactory().getValidator();
    @Test void acceptsNewbornOptionalAddressAndValidAadhaar() {
        var request=request((short)0,"012345678901",LocalDate.of(2026,10,6));
        assertThat(validator.validate(request)).isEmpty();
    }
    @Test void rejectsInvalidAadhaarAndFutureDob() {
        assertThat(validator.validate(request((short)1,"1234ABCD9012",LocalDate.now().plusDays(1))))
                .extracting(v->v.getPropertyPath().toString()).contains("aadhaarNo","dateOfBirth");
    }
    private RegistrationPatientRequest request(short age,String aadhaar,LocalDate dob) {
        return new RegistrationPatientRequest(null,false,"Baby","Test",null,Gender.FEMALE,age,
                "9999999999",null,"baby@example.com",dob,aadhaar);
    }
}
