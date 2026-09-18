package vn.nitrogen.common.web;

import java.net.URI;
import java.util.List;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import vn.nitrogen.common.error.BusinessException;
import vn.nitrogen.common.error.ErrorCode;

/**
 * Chuyển exception nghiệp vụ thành RFC 7807 ProblemDetail.
 *
 * <p>Nguyên tắc log (§18): không ghi Authorization, token, password, answer key
 * hay payload nhạy cảm. Lỗi nghiệp vụ log mức WARN không kèm stacktrace — chúng
 * là kết quả mong đợi (409 khi hai tab cùng ghi một response là hành vi đúng của
 * optimistic lock, không phải sự cố), và stacktrace ở đây chỉ làm nhiễu log lúc
 * mùa thi khi loại 409 này xuất hiện dày.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_BASE = "https://docs.nitrogen.vn/errors/";

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        log.warn("Business error {}: {}", code, ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + code.name().toLowerCase()));
        problem.setProperty("errorCode", code.name());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        ProblemDetail problem = validationProblem("Request validation failed.");
        problem.setProperty("fields", fields);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        List<String> violations = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        ProblemDetail problem = validationProblem("Request validation failed.");
        problem.setProperty("fields", violations);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return validationProblem("Request body is invalid.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);

        // Không để lộ message gốc ra client: nó có thể chứa tên bảng, câu SQL
        // hoặc dữ liệu của người dùng khác.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                ErrorCode.INTERNAL_ERROR.status(), "Đã xảy ra lỗi không mong đợi.");
        problem.setType(URI.create(PROBLEM_BASE + "internal_error"));
        problem.setProperty("errorCode", ErrorCode.INTERNAL_ERROR.name());
        return problem;
    }

    private static ProblemDetail validationProblem(String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ErrorCode.VALIDATION_FAILED.status(), detail);
        problem.setType(URI.create(PROBLEM_BASE + "validation_failed"));
        problem.setProperty("errorCode", ErrorCode.VALIDATION_FAILED.name());
        return problem;
    }
}
