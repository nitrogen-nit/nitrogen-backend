package vn.nitrogen.common.error;

import java.io.Serial;
import org.springframework.http.HttpStatus;

/**
 * Lỗi nghiệp vụ có mã và HTTP status xác định trước.
 *
 * <p>Service ném exception này; {@link vn.nitrogen.common.web.GlobalExceptionHandler}
 * dịch sang ProblemDetail. Module KHÔNG tự dựng ResponseEntity lỗi trong controller.
 */
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public BusinessException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
