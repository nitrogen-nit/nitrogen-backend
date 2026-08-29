package vn.nitrogen.common.web;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
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
}
