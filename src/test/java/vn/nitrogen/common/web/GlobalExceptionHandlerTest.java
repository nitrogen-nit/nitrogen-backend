package vn.nitrogen.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import vn.nitrogen.common.error.BusinessException;
import vn.nitrogen.common.error.ErrorCode;

@Tag("unit")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsBusinessExceptionToProblemDetail() {
        ProblemDetail problem = handler.handleBusinessException(new BusinessException(
                ErrorCode.ATTEMPT_NOT_EDITABLE,
                HttpStatus.CONFLICT,
                "Attempt is closed"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getDetail()).isEqualTo("Attempt is closed");
        assertThat(problem.getType()).isEqualTo(URI.create("https://docs.nitrogen.vn/errors/attempt_not_editable"));
        assertThat(problem.getProperties()).containsEntry("errorCode", "ATTEMPT_NOT_EDITABLE");
    }

    @Test
    void hidesUnexpectedExceptionDetails() {
        ProblemDetail problem = handler.handleUnexpected(new RuntimeException("database driver failed"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).isEqualTo("Đã xảy ra lỗi không mong đợi.");
        assertThat(problem.getDetail()).doesNotContain("driver");
        assertThat(problem.getType()).isEqualTo(URI.create("https://docs.nitrogen.vn/errors/internal_error"));
        assertThat(problem.getProperties()).containsEntry("errorCode", "INTERNAL_ERROR");
    }
}
