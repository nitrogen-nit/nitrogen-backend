package vn.nitrogen.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@Tag("unit")
class BusinessExceptionTest {

    @Test
    void exposesStableErrorContract() {
        BusinessException exception = new BusinessException(
                ErrorCode.ATTEMPT_ALREADY_SUBMITTED,
                HttpStatus.CONFLICT,
                "Attempt was already submitted");

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ATTEMPT_ALREADY_SUBMITTED);
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception).hasMessage("Attempt was already submitted");
    }

    @Test
    void everyErrorCodeHasHttpStatus() {
        assertThat(ErrorCode.values())
                .extracting(ErrorCode::status)
                .doesNotContainNull()
                .contains(HttpStatus.BAD_REQUEST, HttpStatus.CONFLICT, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
